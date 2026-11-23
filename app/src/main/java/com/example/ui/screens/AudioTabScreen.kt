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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AudioSortOption
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaFormatters
import com.example.ui.components.EmptyLibraryState
import com.example.ui.components.MediaArtwork
import com.example.ui.components.StudioSearchBar
import com.example.ui.theme.MonoTimeStyle
import com.example.ui.theme.PulseCoral

@Composable
fun AudioTabScreen(
    tracks: List<LocalMediaItem>,
    searchQuery: String,
    sortOption: AudioSortOption,
    isScanning: Boolean,
    currentPlayingUri: String?,
    isPlaying: Boolean,
    favoriteUris: Set<String>,
    onSearchChange: (String) -> Unit,
    onSortChange: (AudioSortOption) -> Unit,
    onRefresh: () -> Unit,
    onTrackClick: (LocalMediaItem, List<LocalMediaItem>) -> Unit,
    onShuffleAll: (List<LocalMediaItem>) -> Unit,
    onToggleFavorite: (LocalMediaItem) -> Unit,
    onAddToPlaylist: (LocalMediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.audio_header),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${tracks.size} tracks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tracks.isNotEmpty()) {
                    FilledTonalIconButton(
                        onClick = { onShuffleAll(tracks) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("audio_shuffle_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = stringResource(R.string.shuffle_all),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("audio_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.refresh)
                    )
                }
            }
        }

        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }

        // Instant Search Bar
        StudioSearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = stringResource(R.string.audio_search_hint),
            testTag = "audio_search_input",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // Sort Chips Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SortChip(
                    label = stringResource(R.string.sort_name),
                    selected = sortOption == AudioSortOption.NAME,
                    onClick = { onSortChange(AudioSortOption.NAME) },
                    testTag = "sort_chip_name"
                )
            }
            item {
                SortChip(
                    label = stringResource(R.string.sort_date),
                    selected = sortOption == AudioSortOption.DATE_ADDED,
                    onClick = { onSortChange(AudioSortOption.DATE_ADDED) },
                    testTag = "sort_chip_date"
                )
            }
            item {
                SortChip(
                    label = stringResource(R.string.sort_duration),
                    selected = sortOption == AudioSortOption.DURATION,
                    onClick = { onSortChange(AudioSortOption.DURATION) },
                    testTag = "sort_chip_duration"
                )
            }
        }

        if (tracks.isEmpty() && !isScanning) {
            EmptyLibraryState(
                title = stringResource(R.string.empty_audio_title),
                description = stringResource(R.string.empty_audio_desc),
                onRefresh = onRefresh
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(tracks, key = { _, item -> item.uri }) { _, track ->
                    val isCurrent = track.uri == currentPlayingUri
                    val isFav = track.uri in favoriteUris
                    AudioTrackCard(
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        isFavorite = isFav,
                        onClick = { onTrackClick(track, tracks) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = Modifier.testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun AudioTrackCard(
    track: LocalMediaItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    extraMenuActionLabel: String? = null,
    onExtraMenuAction: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("audio_track_card_${track.id}"),
        color = if (isCurrent) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(16.dp),
        border = if (isCurrent) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaArtwork(
                uri = track.albumArtUri,
                isVideo = false,
                contentDescription = track.title,
                modifier = Modifier.size(54.dp),
                cornerRadius = 12.dp,
                isActive = isCurrent,
                isPlaying = isPlaying
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = track.artist.ifBlank { stringResource(R.string.unknown_artist) },
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
                        text = MediaFormatters.formatDuration(track.durationMs),
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
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(16.dp)
                )
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
