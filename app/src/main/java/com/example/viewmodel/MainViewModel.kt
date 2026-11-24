package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import com.example.data.MediaRepository
import com.example.data.MediaStoreScanner
import com.example.data.local.PlaybackHistoryEntity
import com.example.data.local.PlaylistWithCount
import com.example.data.local.PulsePlayDatabase
import com.example.data.model.AudioSortOption
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaFolder
import com.example.data.model.MediaType
import com.example.data.model.SleepTimerOption
import com.example.player.PlaybackManager
import com.example.player.PlaybackUiState
import com.example.util.PermissionUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PulsePlayDatabase.getInstance(application)
    private val scanner = MediaStoreScanner(application)
    private val repository = MediaRepository(scanner, database.mediaDao())
    val playbackManager = PlaybackManager(application, repository)

    private val _hasPermissions = MutableStateFlow(PermissionUtils.hasMediaPermissions(application))
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _audioFiles = MutableStateFlow<List<LocalMediaItem>>(emptyList())
    private val _videoFiles = MutableStateFlow<List<LocalMediaItem>>(emptyList())

    /** URIs the user removed "from the app only". */
    val hiddenUris: StateFlow<Set<String>> = repository.hiddenUris
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val visibleAudio: StateFlow<List<LocalMediaItem>> =
        combine(_audioFiles, hiddenUris) { items, hidden -> items.filter { it.uri !in hidden } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val visibleVideo: StateFlow<List<LocalMediaItem>> =
        combine(_videoFiles, hiddenUris) { items, hidden -> items.filter { it.uri !in hidden } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _audioSearchQuery = MutableStateFlow("")
    val audioSearchQuery: StateFlow<String> = _audioSearchQuery.asStateFlow()

    private val _videoSearchQuery = MutableStateFlow("")
    val videoSearchQuery: StateFlow<String> = _videoSearchQuery.asStateFlow()

    private val _audioSortOption = MutableStateFlow(AudioSortOption.DATE_ADDED)
    val audioSortOption: StateFlow<AudioSortOption> = _audioSortOption.asStateFlow()

    val playbackState: StateFlow<PlaybackUiState> = playbackManager.uiState
    val mediaController: StateFlow<MediaController?> = playbackManager.controller

    val allMediaMap: StateFlow<Map<String, LocalMediaItem>> = combine(
        visibleAudio,
        visibleVideo
    ) { audio, video ->
        (audio + video).associateBy { it.uri }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val filteredAudio: StateFlow<List<LocalMediaItem>> = combine(
        visibleAudio,
        _audioSearchQuery,
        _audioSortOption
    ) { tracks, query, sort ->
        val trimmed = query.trim()
        val matching = if (trimmed.isEmpty()) {
            tracks
        } else {
            tracks.filter {
                it.title.contains(trimmed, ignoreCase = true) ||
                    it.artist.contains(trimmed, ignoreCase = true) ||
                    it.album.contains(trimmed, ignoreCase = true) ||
                    it.folderName.contains(trimmed, ignoreCase = true)
            }
        }
        when (sort) {
            AudioSortOption.NAME -> matching.sortedBy { it.title.lowercase() }
            AudioSortOption.DATE_ADDED -> matching.sortedByDescending { it.dateAddedSec }
            AudioSortOption.DURATION -> matching.sortedByDescending { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredVideos: StateFlow<List<LocalMediaItem>> = combine(
        visibleVideo,
        _videoSearchQuery
    ) { videos, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            videos
        } else {
            videos.filter {
                it.title.contains(trimmed, ignoreCase = true) ||
                    it.folderName.contains(trimmed, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<MediaFolder>> = combine(visibleAudio, visibleVideo) { audio, video ->
        val all = audio + video
        all.groupBy { it.folderPath }
            .map { (path, items) ->
                val audioCount = items.count { it.mediaType == MediaType.AUDIO }
                val videoCount = items.count { it.mediaType == MediaType.VIDEO }
                val previewItem = items.firstOrNull { it.mediaType == MediaType.VIDEO }
                    ?: items.firstOrNull { it.albumArtUri != null }
                    ?: items.firstOrNull()
                MediaFolder(
                    path = path,
                    name = items.first().folderName,
                    audioCount = audioCount,
                    videoCount = videoCount,
                    totalCount = items.size,
                    previewUri = previewItem?.albumArtUri,
                    previewIsVideo = previewItem?.mediaType == MediaType.VIDEO
                )
            }
            .sortedByDescending { it.totalCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customPlaylists: StateFlow<List<PlaylistWithCount>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteUris: StateFlow<Set<String>> = repository.favoriteUris
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val favoriteItems: StateFlow<List<LocalMediaItem>> = combine(
        repository.favoriteUris,
        allMediaMap
    ) { uris, lookup ->
        uris.mapNotNull { lookup[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentHistoryMap: StateFlow<Map<String, PlaybackHistoryEntity>> = repository.recentHistory
        .map { list -> list.associateBy { it.mediaUri } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val recentlyPlayedItems: StateFlow<List<LocalMediaItem>> = combine(
        repository.recentHistory,
        allMediaMap
    ) { history, lookup ->
        history.mapNotNull { lookup[it.mediaUri] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (_hasPermissions.value) {
            refreshLibrary()
        }
    }

    fun onPermissionsUpdated() {
        val granted = PermissionUtils.hasMediaPermissions(getApplication())
        _hasPermissions.value = granted
        if (granted) {
            refreshLibrary()
        }
    }

    fun refreshLibrary() {
        if (!PermissionUtils.hasMediaPermissions(getApplication())) {
            _hasPermissions.value = false
            return
        }
        _hasPermissions.value = true
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val audioDeferred = async { repository.scanAudio() }
                val videoDeferred = async { repository.scanVideo() }
                val audio = audioDeferred.await()
                val video = videoDeferred.await()
                _audioFiles.value = audio
                _videoFiles.value = video
                playbackManager.updateMediaCatalog(audio + video)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun updateAudioSearch(query: String) {
        _audioSearchQuery.value = query
    }

    fun updateVideoSearch(query: String) {
        _videoSearchQuery.value = query
    }

    fun updateAudioSort(option: AudioSortOption) {
        _audioSortOption.value = option
    }

    fun playItem(
        item: LocalMediaItem,
        queue: List<LocalMediaItem>,
        asAudioOnly: Boolean = false
    ) {
        val index = queue.indexOfFirst { it.uri == item.uri }.takeIf { it >= 0 } ?: 0
        val effectiveQueue = if (queue.isEmpty()) listOf(item) else queue
        viewModelScope.launch {
            val resumePos = repository.getLastPlayedPosition(item.uri)
            playbackManager.playQueue(
                items = effectiveQueue,
                startIndex = index,
                startPositionMs = resumePos,
                asAudioOnly = asAudioOnly
            )
        }
    }

    fun shuffleAndPlay(items: List<LocalMediaItem>) {
        if (items.isEmpty()) return
        val shuffled = items.shuffled()
        playbackManager.playQueue(
            items = shuffled,
            startIndex = 0,
            startPositionMs = 0L,
            asAudioOnly = shuffled.first().mediaType == MediaType.VIDEO
        )
    }

    fun getItemsForFolder(folderPath: String): Flow<List<LocalMediaItem>> {
        return combine(visibleAudio, visibleVideo) { audio, video ->
            (audio + video)
                .filter { it.folderPath == folderPath }
                .sortedByDescending { it.dateAddedSec }
        }
    }

    fun observePlaylistItems(playlistId: Long): Flow<List<LocalMediaItem>> {
        return combine(
            repository.observePlaylistItemUris(playlistId),
            allMediaMap
        ) { uris, lookup ->
            uris.mapNotNull { lookup[it] }
        }
    }

    fun observePlaylistIdsForMedia(mediaUri: String): Flow<Set<Long>> {
        return repository.observePlaylistIdsForMedia(mediaUri).map { it.toSet() }
    }

    fun createPlaylist(name: String, initialMediaUri: String? = null) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            if (id > 0L && initialMediaUri != null) {
                repository.addItemToPlaylist(id, initialMediaUri)
            }
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun toggleMediaInPlaylist(playlistId: Long, mediaUri: String, currentlyInPlaylist: Boolean) {
        viewModelScope.launch {
            if (currentlyInPlaylist) {
                repository.removeItemFromPlaylist(playlistId, mediaUri)
            } else {
                repository.addItemToPlaylist(playlistId, mediaUri)
            }
        }
    }

    fun removeMediaFromPlaylist(playlistId: Long, mediaUri: String) {
        viewModelScope.launch {
            repository.removeItemFromPlaylist(playlistId, mediaUri)
        }
    }

    fun toggleFavorite(mediaUri: String) {
        viewModelScope.launch {
            repository.toggleFavorite(mediaUri)
        }
    }

    /** Removes the item from the library view only; the file stays on the phone. */
    fun hideFromApp(item: LocalMediaItem) {
        viewModelScope.launch { repository.hideMedia(item.uri) }
    }

    fun unhideAll() {
        viewModelScope.launch { repository.unhideAll() }
    }

    /** Called after the file was really deleted from the phone. */
    fun onDeletedFromDevice(item: LocalMediaItem) {
        _audioFiles.value = _audioFiles.value.filterNot { it.uri == item.uri }
        _videoFiles.value = _videoFiles.value.filterNot { it.uri == item.uri }
        viewModelScope.launch {
            repository.forgetDeletedMedia(item.uri)
            refreshLibrary()
        }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        playbackManager.setSleepTimer(option)
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
