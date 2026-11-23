package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaFormatters
import com.example.data.model.MediaType
import com.example.data.model.RepeatModeOption
import com.example.data.model.SleepTimerOption
import com.example.player.PlaybackUiState
import com.example.ui.components.FavoriteIconButton
import com.example.ui.components.MediaArtwork
import com.example.ui.components.PlaylistAddIconButton
import com.example.ui.theme.MonoTimeStyle
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseCyan

private val NowPlayingSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    isFavorite: Boolean,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onToggleFavorite: (LocalMediaItem) -> Unit,
    onAddToPlaylist: (LocalMediaItem) -> Unit,
    onSwitchToVideo: () -> Unit
) {
    BackHandler(onBack = onCollapse)

    val media = playbackState.currentMedia ?: return
    val durationMs = playbackState.durationMs.coerceAtLeast(1L)

    var isSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var timerMenuOpen by remember { mutableStateOf(false) }

    val displayedPositionMs = if (isSeeking) {
        (sliderPosition * durationMs).toLong()
    } else {
        playbackState.positionMs.coerceIn(0L, durationMs)
    }
    val remainingMs = (durationMs - displayedPositionMs).coerceAtLeast(0L)
    val sliderValue = if (isSeeking) {
        sliderPosition
    } else {
        (displayedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PulseCoral.copy(alpha = 0.22f),
                        Color(0xFF101320),
                        ObsidianBg
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_collapse")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.collapse_player),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.now_playing).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = media.folderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = PulseCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (media.mediaType == MediaType.VIDEO) {
                    FilledTonalButton(
                        onClick = onSwitchToVideo,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("now_playing_switch_to_video")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = PulseCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.switch_to_video),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Large Studio Album Art Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = PulseCoral.copy(alpha = 0.4f),
                        spotColor = PulseCoral.copy(alpha = 0.4f)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                        RoundedCornerShape(28.dp)
                    )
                    .clip(RoundedCornerShape(28.dp))
            ) {
                MediaArtwork(
                    uri = media.albumArtUri,
                    isVideo = media.mediaType == MediaType.VIDEO,
                    contentDescription = media.title,
                    cornerRadius = 28.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title + Artist + Favorite + Add to Playlist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = media.artist.ifBlank { stringResource(R.string.unknown_artist) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FavoriteIconButton(
                    isFavorite = isFavorite,
                    onClick = { onToggleFavorite(media) },
                    modifier = Modifier.testTag("now_playing_favorite_button")
                )

                PlaylistAddIconButton(
                    onClick = { onAddToPlaylist(media) },
                    modifier = Modifier.testTag("now_playing_add_playlist_button")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Seekbar + Elapsed & Remaining Time
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    isSeeking = true
                    sliderPosition = value
                },
                onValueChangeFinished = {
                    val seekTarget = (sliderPosition * durationMs).toLong()
                    onSeekTo(seekTarget)
                    isSeeking = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("now_playing_seekbar"),
                colors = SliderDefaults.colors(
                    thumbColor = PulseCoral,
                    activeTrackColor = PulseCoral,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = MediaFormatters.formatDuration(displayedPositionMs),
                    style = MonoTimeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "-${MediaFormatters.formatDuration(remainingMs)}",
                    style = MonoTimeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_shuffle")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = stringResource(R.string.shuffle),
                        tint = if (playbackState.isShuffleEnabled) PulseCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("now_playing_previous")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.previous_track),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Glowing Primary Play/Pause Circle
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            ambientColor = PulseCoral,
                            spotColor = PulseCoral
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PulseCoral, Color(0xFFFF7D45))
                            )
                        )
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("now_playing_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.play_pause),
                        tint = Color(0xFF1A0703),
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("now_playing_next")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.next_track),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_repeat")
                ) {
                    val repeatIcon = if (playbackState.repeatMode == RepeatModeOption.ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    }
                    val repeatTint = if (playbackState.repeatMode != RepeatModeOption.OFF) {
                        PulseCyan
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val repeatCd = when (playbackState.repeatMode) {
                        RepeatModeOption.OFF -> stringResource(R.string.repeat_off)
                        RepeatModeOption.ALL -> stringResource(R.string.repeat_all)
                        RepeatModeOption.ONE -> stringResource(R.string.repeat_one)
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = repeatCd,
                        tint = repeatTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Utility Pill Bar: Playback Speed & Sleep Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Playback Speed Selector
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { speedMenuOpen = true }
                            .testTag("now_playing_speed_button"),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (playbackState.playbackSpeed != 1.0f) {
                                PulseCoral.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = stringResource(R.string.playback_speed),
                                tint = if (playbackState.playbackSpeed != 1.0f) PulseCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = MediaFormatters.formatSpeed(playbackState.playbackSpeed),
                                style = MonoTimeStyle.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = speedMenuOpen,
                        onDismissRequest = { speedMenuOpen = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        NowPlayingSpeeds.forEach { speed ->
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

                // Sleep Timer Selector
                Box(modifier = Modifier.weight(1f)) {
                    val isTimerActive = playbackState.sleepTimerRemainingMs != null
                    val timerLabel = playbackState.sleepTimerRemainingMs?.let { rem ->
                        MediaFormatters.formatDuration(rem)
                    } ?: stringResource(R.string.sleep_timer)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { timerMenuOpen = true }
                            .testTag("now_playing_sleep_timer_button"),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isTimerActive) PulseCyan.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = stringResource(R.string.sleep_timer),
                                tint = if (isTimerActive) PulseCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = timerLabel,
                                style = if (isTimerActive) MonoTimeStyle else MaterialTheme.typography.labelMedium,
                                color = if (isTimerActive) PulseCyan else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = timerMenuOpen,
                        onDismissRequest = { timerMenuOpen = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        val timerEntries = listOf(
                            SleepTimerOption.OFF to stringResource(R.string.sleep_timer_off),
                            SleepTimerOption.MIN_15 to stringResource(R.string.sleep_timer_15),
                            SleepTimerOption.MIN_30 to stringResource(R.string.sleep_timer_30),
                            SleepTimerOption.MIN_60 to stringResource(R.string.sleep_timer_60)
                        )
                        timerEntries.forEach { (option, label) ->
                            val selected = playbackState.sleepTimerOption == option
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        color = if (selected) PulseCyan else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    timerMenuOpen = false
                                    onSelectSleepTimer(option)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
