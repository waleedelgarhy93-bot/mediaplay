package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.R
import com.example.data.local.PlaylistWithCount
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaType
import com.example.player.PlaybackUiState
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseCyan

@Composable
fun rememberVideoImageLoader(): ImageLoader {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}

@Composable
fun MediaArtwork(
    uri: String?,
    isVideo: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    isActive: Boolean = false,
    isPlaying: Boolean = false
) {
    val context = LocalContext.current
    val imageLoader = rememberVideoImageLoader()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (!uri.isNullOrBlank()) {
            val request = remember(uri, isVideo) {
                ImageRequest.Builder(context)
                    .data(uri)
                    .apply {
                        if (isVideo) {
                            videoFrameMillis(1_500L)
                        }
                    }
                    .error(R.drawable.img_album_placeholder)
                    .placeholder(R.drawable.img_album_placeholder)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_album_placeholder),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.52f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedEqualizerBars(isPlaying = isPlaying)
            }
        }
    }
}

@Composable
fun AnimatedEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = PulseCyan
) {
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val heights = if (isPlaying) listOf(bar1, bar2, bar3) else listOf(0.35f, 0.55f, 0.35f)
    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height((18.dp * fraction).coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun StudioSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackUiState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = playbackState.currentMedia ?: return
    val progress = if (playbackState.durationMs > 0L) {
        (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onExpand)
            .testTag("mini_player_bar"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Razor-thin luminous progress line along the top edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(PulseCoral, PulseCyan)
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaArtwork(
                    uri = item.albumArtUri,
                    isVideo = item.mediaType == MediaType.VIDEO,
                    contentDescription = item.title,
                    modifier = Modifier.size(46.dp),
                    cornerRadius = 10.dp,
                    isActive = true,
                    isPlaying = playbackState.isPlaying
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (item.mediaType == MediaType.VIDEO && playbackState.isAudioOnlyVideoMode) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = PulseCyan,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = if (item.mediaType == MediaType.VIDEO && playbackState.isAudioOnlyVideoMode) {
                                stringResource(R.string.audio_mode_badge)
                            } else {
                                item.artist.ifBlank { stringResource(R.string.unknown_artist) }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.mediaType == MediaType.VIDEO && playbackState.isAudioOnlyVideoMode) {
                                PulseCyan
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_player_play_pause")
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.play_pause),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.next_track),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryState(
    title: String,
    description: String,
    onRefresh: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_library),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRefresh != null) {
            Spacer(modifier = Modifier.height(20.dp))
            FilledTonalButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("empty_state_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.refresh))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
    mediaItem: LocalMediaItem,
    playlists: List<PlaylistWithCount>,
    selectedPlaylistIds: Set<Long>,
    onTogglePlaylist: (playlistId: Long, currentlyInPlaylist: Boolean) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.add_to_playlist),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mediaItem.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FilledTonalButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("sheet_new_playlist_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.new_playlist))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (playlists.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_playlists_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        val isInPlaylist = playlist.id in selectedPlaylistIds
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onTogglePlaylist(playlist.id, isInPlaylist)
                                },
                            color = if (isInPlaylist) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint = if (isInPlaylist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${playlist.itemCount} items",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (isInPlaylist) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isInPlaylist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = stringResource(R.string.new_playlist),
            initialName = "",
            confirmLabel = stringResource(R.string.create),
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.playlist_name_hint)) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("playlist_dialog_confirm")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun FavoriteIconButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(R.string.favorite),
            tint = if (isFavorite) PulseCoral else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PlaylistAddIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
            contentDescription = stringResource(R.string.add_to_playlist),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
