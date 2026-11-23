package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaFormatters
import com.example.player.PlaybackUiState
import com.example.ui.components.FavoriteIconButton
import com.example.ui.components.PlaylistAddIconButton
import com.example.ui.theme.MonoTimeStyle
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseCyan
import kotlinx.coroutines.delay

private val VideoPlaybackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

private enum class GestureHudType { NONE, BRIGHTNESS, VOLUME }
private enum class DoubleTapSeekFeedback { NONE, REWIND_10, FORWARD_10 }

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    mediaController: MediaController?,
    playbackState: PlaybackUiState,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onPlayAsAudio: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onToggleFavorite: (LocalMediaItem) -> Unit,
    onAddToPlaylist: (LocalMediaItem) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isLandscapeLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Brightness & Volume Gesture HUD state
    var gestureHudType by remember { mutableStateOf(GestureHudType.NONE) }
    var brightnessLevel by remember {
        val current = activity?.window?.attributes?.screenBrightness ?: 0.5f
        mutableFloatStateOf(if (current < 0f) 0.5f else current.coerceIn(0.05f, 1f))
    }
    val maxVolume = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var volumeLevel by remember {
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mutableFloatStateOf((currentVol.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f))
    }

    // Double-tap seek feedback HUD state
    var seekFeedback by remember { mutableStateOf(DoubleTapSeekFeedback.NONE) }

    // Seekbar drag state
    var isSeeking by remember { mutableStateOf(false) }
    var sliderPos by remember { mutableFloatStateOf(0f) }

    val handleExit = {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onClose()
    }

    BackHandler(onBack = handleExit)

    // Immersive full-screen system bars & restore orientation on leave
    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Auto-hide controls after 3.5s while playing
    LaunchedEffect(controlsVisible, playbackState.isPlaying, isSeeking, speedMenuOpen) {
        if (controlsVisible && playbackState.isPlaying && !isSeeking && !speedMenuOpen) {
            delay(3_500L)
            controlsVisible = false
        }
    }

    // Auto-dismiss double-tap seek HUD
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != DoubleTapSeekFeedback.NONE) {
            delay(700L)
            seekFeedback = DoubleTapSeekFeedback.NONE
        }
    }

    val currentMedia = playbackState.currentMedia
    val durationMs = playbackState.durationMs.coerceAtLeast(1L)
    val displayedPosMs = if (isSeeking) {
        (sliderPos * durationMs).toLong()
    } else {
        playbackState.positionMs.coerceIn(0L, durationMs)
    }
    val sliderFraction = if (isSeeking) {
        sliderPos
    } else {
        (displayedPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
    ) {
        // Media3 ExoPlayer Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    keepScreenOn = true
                    this.resizeMode = resizeMode
                    player = mediaController
                }
            },
            update = { playerView ->
                if (playerView.player !== mediaController) {
                    playerView.player = mediaController
                }
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("video_player_surface")
        )

        // Full-Screen Gesture Layer: Single-Tap, Double-Tap Seek, Left Brightness, Right Volume
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(containerSize) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                        },
                        onDoubleTap = { offset ->
                            val halfWidth = (containerSize.width / 2f).coerceAtLeast(1f)
                            if (offset.x < halfWidth) {
                                onSeekBy(-10_000L)
                                seekFeedback = DoubleTapSeekFeedback.REWIND_10
                            } else {
                                onSeekBy(10_000L)
                                seekFeedback = DoubleTapSeekFeedback.FORWARD_10
                            }
                        }
                    )
                }
                .pointerInput(containerSize, maxVolume) {
                    var dragIsLeft = true
                    detectVerticalDragGestures(
                        onDragStart = { startOffset ->
                            val halfWidth = (containerSize.width / 2f).coerceAtLeast(1f)
                            dragIsLeft = startOffset.x < halfWidth
                            if (dragIsLeft) {
                                gestureHudType = GestureHudType.BRIGHTNESS
                            } else {
                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                volumeLevel = (currentVol.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)
                                gestureHudType = GestureHudType.VOLUME
                            }
                        },
                        onDragEnd = { gestureHudType = GestureHudType.NONE },
                        onDragCancel = { gestureHudType = GestureHudType.NONE },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val height = containerSize.height.coerceAtLeast(1).toFloat()
                            val deltaFraction = (-dragAmount / (height * 0.7f))
                            if (dragIsLeft) {
                                brightnessLevel = (brightnessLevel + deltaFraction).coerceIn(0.05f, 1f)
                                activity?.window?.let { win ->
                                    val lp = win.attributes
                                    lp.screenBrightness = brightnessLevel
                                    win.attributes = lp
                                }
                            } else {
                                volumeLevel = (volumeLevel + deltaFraction).coerceIn(0f, 1f)
                                val targetVol = (volumeLevel * maxVolume).toInt().coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            }
                        }
                    )
                }
        )

        // Brightness / Volume Gesture HUD Pill
        AnimatedVisibility(
            visible = gestureHudType != GestureHudType.NONE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val isBrightness = gestureHudType == GestureHudType.BRIGHTNESS
            val level = if (isBrightness) brightnessLevel else volumeLevel
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(170.dp)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isBrightness) Icons.Rounded.BrightnessHigh else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = if (isBrightness) stringResource(R.string.brightness) else stringResource(R.string.volume),
                        tint = if (isBrightness) PulseCoral else PulseCyan,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { level },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isBrightness) PulseCoral else PulseCyan,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${(level * 100).toInt()}%",
                        style = MonoTimeStyle,
                        color = Color.White
                    )
                }
            }
        }

        // Double-Tap +/-10s Seek HUD Feedback
        AnimatedVisibility(
            visible = seekFeedback != DoubleTapSeekFeedback.NONE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(
                    if (seekFeedback == DoubleTapSeekFeedback.REWIND_10) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    }
                )
                .padding(horizontal = 36.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (seekFeedback == DoubleTapSeekFeedback.REWIND_10) {
                            Icons.Rounded.Replay10
                        } else {
                            Icons.Rounded.Forward10
                        },
                        contentDescription = null,
                        tint = PulseCyan,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (seekFeedback == DoubleTapSeekFeedback.REWIND_10) "-10s" else "+10s",
                        style = MonoTimeStyle.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // Controls Overlay (Top bar, Center Transport, Bottom Seekbar & Utilities)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.80f)
                            )
                        )
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = handleExit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("video_player_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }

                    Text(
                        text = currentMedia?.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Play as Audio (Background Play) Pill Button
                    Surface(
                        onClick = {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            onPlayAsAudio()
                        },
                        color = PulseCyan.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("video_play_as_audio_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = stringResource(R.string.play_as_audio),
                                tint = PulseCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.play_as_audio),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }

                    if (currentMedia != null) {
                        FavoriteIconButton(
                            isFavorite = isFavorite,
                            onClick = { onToggleFavorite(currentMedia) }
                        )
                        PlaylistAddIconButton(
                            onClick = { onAddToPlaylist(currentMedia) }
                        )
                    }
                }

                // Center Play / Seek 10s Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSeekBy(-10_000L) },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .testTag("video_rewind_10")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = stringResource(R.string.seek_back_10),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PulseCoral)
                            .testTag("video_play_pause")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.play_pause),
                            tint = Color(0xFF1A0703),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSeekBy(10_000L) },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .testTag("video_forward_10")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = stringResource(R.string.seek_forward_10),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Bar: Seekbar + Speed + Aspect Ratio + Rotation Toggle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = MediaFormatters.formatDuration(displayedPosMs),
                            style = MonoTimeStyle,
                            color = Color.White
                        )
                        Slider(
                            value = sliderFraction,
                            onValueChange = { value ->
                                isSeeking = true
                                sliderPos = value
                            },
                            onValueChangeFinished = {
                                onSeekTo((sliderPos * durationMs).toLong())
                                isSeeking = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                                .testTag("video_seekbar"),
                            colors = SliderDefaults.colors(
                                thumbColor = PulseCoral,
                                activeTrackColor = PulseCoral,
                                inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                            )
                        )
                        Text(
                            text = MediaFormatters.formatDuration(durationMs),
                            style = MonoTimeStyle,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Speed Selector (0.5x to 2.0x)
                        Box {
                            Surface(
                                onClick = { speedMenuOpen = true },
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("video_speed_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Speed,
                                        contentDescription = stringResource(R.string.playback_speed),
                                        tint = PulseCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = MediaFormatters.formatSpeed(playbackState.playbackSpeed),
                                        style = MonoTimeStyle,
                                        color = Color.White
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = speedMenuOpen,
                                onDismissRequest = { speedMenuOpen = false },
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                VideoPlaybackSpeeds.forEach { speed ->
                                    val selected = kotlin.math.abs(playbackState.playbackSpeed - speed) < 0.02f
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = MediaFormatters.formatSpeed(speed),
                                                color = if (selected) PulseCoral else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            speedMenuOpen = false
                                            onSelectSpeed(speed)
                                        }
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Aspect Ratio / Crop Toggle
                            IconButton(
                                onClick = {
                                    resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    } else {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("video_aspect_ratio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AspectRatio,
                                    contentDescription = stringResource(R.string.resize_video),
                                    tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) PulseCyan else Color.White
                                )
                            }

                            // Screen Rotation Toggle
                            IconButton(
                                onClick = {
                                    isLandscapeLocked = !isLandscapeLocked
                                    activity?.requestedOrientation = if (isLandscapeLocked) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("video_rotate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ScreenRotation,
                                    contentDescription = stringResource(R.string.rotate_screen),
                                    tint = if (isLandscapeLocked) PulseCoral else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
