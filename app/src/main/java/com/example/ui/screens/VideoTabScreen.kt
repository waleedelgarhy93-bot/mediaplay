package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaFormatters
import com.example.ui.components.EmptyLibraryState
import com.example.ui.components.MediaArtwork
import com.example.ui.components.StudioSearchBar
import com.example.ui.theme.MonoTimeStyle
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseCyan

@Composable
fun VideoTabScreen(
    videos: List<LocalMediaItem>,
    searchQuery: String,
    isScanning: Boolean,
    currentPlayingUri: String?,
    favoriteUris: Set<String>,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onVideoClick: (LocalMediaItem, List<LocalMediaItem>) -> Unit,
    onPlayAsAudio: (LocalMediaItem, List<LocalMediaItem>) -> Unit,
    onToggleFavorite: (LocalMediaItem) -> Unit,
    onAddToPlaylist: (LocalMediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.video_header),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${videos.size} videos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("video_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.refresh)
                )
            }
        }

        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }

        StudioSearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = stringResource(R.string.video_search_hint),
            testTag = "video_search_input",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (videos.isEmpty() && !isScanning) {
            EmptyLibraryState(
                title = stringResource(R.string.empty_video_title),
                description = stringResource(R.string.empty_video_desc),
                onRefresh = onRefresh
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 120.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(videos, key = { it.uri }) { video ->
                    val isCurrent = video.uri == currentPlayingUri
                    val isFav = video.uri in favoriteUris
                    VideoItemCard(
                        video = video,
                        isCurrent = isCurrent,
                        isFavorite = isFav,
                        onClick = { onVideoClick(video, videos) },
                        onPlayAsAudio = { onPlayAsAudio(video, videos) },
                        onToggleFavorite = { onToggleFavorite(video) },
                        onAddToPlaylist = { onAddToPlaylist(video) }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoItemCard(
    video: LocalMediaItem,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onPlayAsAudio: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    extraMenuActionLabel: String? = null,
    onExtraMenuAction: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("video_card_${video.id}"),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCurrent) {
                PulseCyan.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                MediaArtwork(
                    uri = video.uri,
                    isVideo = true,
                    contentDescription = video.title,
                    cornerRadius = 0.dp,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle bottom scrim for crisp badge legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )

                // Center Play Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Top-left resolution pill
                if (video.resolutionLabel.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = video.resolutionLabel,
                            style = MonoTimeStyle,
                            color = PulseCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Bottom-right duration badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = MediaFormatters.formatDuration(video.durationMs),
                        style = MonoTimeStyle,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isCurrent) PulseCyan else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = video.folderName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = MediaFormatters.formatFileSize(video.sizeBytes),
                            style = MonoTimeStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = stringResource(R.string.favorite),
                        tint = PulseCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.play_as_audio)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Headphones,
                                    contentDescription = null,
                                    tint = PulseCyan
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onPlayAsAudio()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.favorite)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) PulseCoral else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleFavorite()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_playlist)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onAddToPlaylist()
                            }
                        )
                        if (extraMenuActionLabel != null && onExtraMenuAction != null) {
                            DropdownMenuItem(
                                text = { Text(extraMenuActionLabel) },
                                onClick = {
                                    menuExpanded = false
                                    onExtraMenuAction()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
