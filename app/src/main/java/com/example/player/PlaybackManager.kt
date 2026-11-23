package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.MediaRepository
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaType
import com.example.data.model.RepeatModeOption
import com.example.data.model.SleepTimerOption
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentMedia: LocalMediaItem? = null,
    val queue: List<LocalMediaItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val isShuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    val sleepTimerRemainingMs: Long? = null,
    val isAudioOnlyVideoMode: Boolean = false
)

class PlaybackManager(
    context: Context,
    private val repository: MediaRepository
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaLookup: Map<String, LocalMediaItem> = emptyMap()
    private var pendingAction: ((MediaController) -> Unit)? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastSavedTickMs: Long = 0L

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressTicker()
            } else {
                persistCurrentPosition()
                stopProgressTicker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncFromController()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            persistCurrentPosition()
            syncFromController()
            val currentUri = mediaItem?.mediaId ?: return
            val duration = _controller.value?.duration?.coerceAtLeast(0L)
                ?: mediaLookup[currentUri]?.durationMs ?: 0L
            scope.launch(Dispatchers.IO) {
                repository.savePlaybackPosition(currentUri, 0L, duration)
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.update { it.copy(repeatMode = repeatMode.toRepeatOption()) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _uiState.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }
    }

    init {
        connectController()
    }

    private fun connectController() {
        val token = SessionToken(
            appContext,
            ComponentName(appContext, PulsePlayMediaService::class.java)
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val mediaController = future.get()
                    _controller.value = mediaController
                    mediaController.addListener(playerListener)
                    syncFromController()
                    if (mediaController.isPlaying) {
                        startProgressTicker()
                    }
                    pendingAction?.invoke(mediaController)
                    pendingAction = null
                } catch (_: Exception) {
                    // Service connection failed or cancelled
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun updateMediaCatalog(allItems: List<LocalMediaItem>) {
        mediaLookup = allItems.associateBy { it.uri }
        syncFromController()
    }

    fun playQueue(
        items: List<LocalMediaItem>,
        startIndex: Int,
        startPositionMs: Long = 0L,
        asAudioOnly: Boolean = false
    ) {
        if (items.isEmpty() || startIndex !in items.indices) return
        mediaLookup = mediaLookup + items.associateBy { it.uri }

        val targetItem = items[startIndex]
        _uiState.update {
            it.copy(
                queue = items,
                currentIndex = startIndex,
                currentMedia = targetItem,
                durationMs = targetItem.durationMs,
                positionMs = startPositionMs,
                isAudioOnlyVideoMode = asAudioOnly
            )
        }

        val mediaItems = items.map { it.toMedia3Item() }
        withController { ctrl ->
            ctrl.setMediaItems(mediaItems, startIndex, startPositionMs)
            ctrl.prepare()
            ctrl.play()
        }

        scope.launch(Dispatchers.IO) {
            repository.savePlaybackPosition(targetItem.uri, startPositionMs, targetItem.durationMs)
        }
    }

    fun setAudioOnlyMode(audioOnly: Boolean) {
        _uiState.update { it.copy(isAudioOnlyVideoMode = audioOnly) }
    }

    fun togglePlayPause() {
        withController { ctrl ->
            if (ctrl.isPlaying) {
                ctrl.pause()
            } else {
                if (ctrl.playbackState == Player.STATE_ENDED) {
                    ctrl.seekTo(0L)
                }
                ctrl.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        _uiState.update { it.copy(positionMs = clamped) }
        withController { ctrl ->
            ctrl.seekTo(clamped)
        }
    }

    fun seekBy(deltaMs: Long) {
        val ctrl = _controller.value ?: return
        val duration = ctrl.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
        val target = (ctrl.currentPosition + deltaMs).coerceIn(0L, duration.coerceAtLeast(0L))
        seekTo(target)
    }

    fun skipToNext() {
        withController { ctrl ->
            if (ctrl.hasNextMediaItem()) {
                ctrl.seekToNextMediaItem()
                ctrl.play()
            } else if (ctrl.mediaItemCount > 0) {
                ctrl.seekTo(0, 0L)
                ctrl.play()
            }
        }
    }

    fun skipToPrevious() {
        withController { ctrl ->
            if (ctrl.currentPosition > 3_500L) {
                ctrl.seekTo(0L)
            } else if (ctrl.hasPreviousMediaItem()) {
                ctrl.seekToPreviousMediaItem()
                ctrl.play()
            } else {
                ctrl.seekTo(0L)
            }
        }
    }

    fun cycleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            RepeatModeOption.OFF -> RepeatModeOption.ALL
            RepeatModeOption.ALL -> RepeatModeOption.ONE
            RepeatModeOption.ONE -> RepeatModeOption.OFF
        }
        _uiState.update { it.copy(repeatMode = next) }
        withController { ctrl ->
            ctrl.repeatMode = when (next) {
                RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
                RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
                RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
            }
        }
    }

    fun toggleShuffle() {
        val next = !_uiState.value.isShuffleEnabled
        _uiState.update { it.copy(isShuffleEnabled = next) }
        withController { ctrl ->
            ctrl.shuffleModeEnabled = next
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _uiState.update { it.copy(playbackSpeed = clamped) }
        withController { ctrl ->
            ctrl.setPlaybackSpeed(clamped)
        }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        if (option == SleepTimerOption.OFF || option.minutes == null) {
            _uiState.update {
                it.copy(
                    sleepTimerOption = SleepTimerOption.OFF,
                    sleepTimerRemainingMs = null
                )
            }
            return
        }
        val totalMs = option.minutes * 60_000L
        _uiState.update {
            it.copy(
                sleepTimerOption = option,
                sleepTimerRemainingMs = totalMs
            )
        }
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0L && isActive) {
                delay(1_000L)
                remaining = (remaining - 1_000L).coerceAtLeast(0L)
                _uiState.update { it.copy(sleepTimerRemainingMs = remaining) }
            }
            if (isActive) {
                _controller.value?.pause()
                _uiState.update {
                    it.copy(
                        sleepTimerOption = SleepTimerOption.OFF,
                        sleepTimerRemainingMs = null
                    )
                }
            }
        }
    }

    private fun withController(action: (MediaController) -> Unit) {
        val ctrl = _controller.value
        if (ctrl != null) {
            action(ctrl)
        } else {
            pendingAction = action
        }
    }

    private fun syncFromController() {
        val ctrl = _controller.value ?: return
        val currentId = ctrl.currentMediaItem?.mediaId
        val matchedItem = currentId?.let { mediaLookup[it] } ?: _uiState.value.currentMedia
        val duration = ctrl.duration.takeIf { it > 0L } ?: matchedItem?.durationMs ?: 0L
        val pos = ctrl.currentPosition.coerceAtLeast(0L)

        _uiState.update { state ->
            state.copy(
                currentMedia = matchedItem,
                currentIndex = ctrl.currentMediaItemIndex,
                isPlaying = ctrl.isPlaying,
                positionMs = pos,
                durationMs = duration,
                repeatMode = ctrl.repeatMode.toRepeatOption(),
                isShuffleEnabled = ctrl.shuffleModeEnabled,
                playbackSpeed = ctrl.playbackParameters.speed
            )
        }
    }

    private fun startProgressTicker() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val ctrl = _controller.value
                if (ctrl != null) {
                    val pos = ctrl.currentPosition.coerceAtLeast(0L)
                    val dur = ctrl.duration.takeIf { it > 0L } ?: _uiState.value.durationMs
                    _uiState.update { it.copy(positionMs = pos, durationMs = dur) }

                    val now = System.currentTimeMillis()
                    if (now - lastSavedTickMs >= 5_000L) {
                        lastSavedTickMs = now
                        persistCurrentPosition()
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun persistCurrentPosition() {
        val ctrl = _controller.value ?: return
        val uri = ctrl.currentMediaItem?.mediaId ?: return
        val pos = ctrl.currentPosition.coerceAtLeast(0L)
        val dur = ctrl.duration.takeIf { it > 0L } ?: _uiState.value.durationMs
        scope.launch(Dispatchers.IO) {
            repository.savePlaybackPosition(uri, pos, dur)
        }
    }

    fun release() {
        persistCurrentPosition()
        stopProgressTicker()
        sleepTimerJob?.cancel()
        _controller.value?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        _controller.value = null
        scope.cancel()
    }

    private fun LocalMediaItem.toMedia3Item(): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist.ifBlank { if (mediaType == MediaType.VIDEO) folderName else "Unknown Artist" })
            .setAlbumTitle(album.ifBlank { folderName })

        albumArtUri?.let { art ->
            metadataBuilder.setArtworkUri(Uri.parse(art))
        }

        return MediaItem.Builder()
            .setMediaId(uri)
            .setUri(Uri.parse(uri))
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun Int.toRepeatOption(): RepeatModeOption = when (this) {
        Player.REPEAT_MODE_ALL -> RepeatModeOption.ALL
        Player.REPEAT_MODE_ONE -> RepeatModeOption.ONE
        else -> RepeatModeOption.OFF
    }
}
