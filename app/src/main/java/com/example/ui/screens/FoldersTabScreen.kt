package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaFolder
import com.example.data.model.MediaType
import com.example.ui.components.EmptyLibraryState
import com.example.ui.theme.PulseAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseCyan

@Composable
fun FoldersTabScreen(
    folders: List<MediaFolder>,
    onRefresh: () -> Unit,
    onFolderClick: (MediaFolder) -> Unit,
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
                    text = stringResource(R.string.folders_header),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${folders.size} folders",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("folders_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.refresh)
                )
            }
        }

        if (folders.isEmpty()) {
            EmptyLibraryState(
                title = stringResource(R.string.empty_folders_title),
                description = stringResource(R.string.empty_folders_desc),
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(folders, key = { it.path }) { folder ->
                    FolderCard(
                        folder = folder,
                        onClick = { onFolderClick(folder) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderCard(
    folder: MediaFolder,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("folder_card_${folder.name}"),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PulseAmber.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = folder.name,
                    tint = PulseAmber,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (folder.audioCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = PulseCoral,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${folder.audioCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PulseCoral
                            )
                        }
                    }
                    if (folder.videoCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Videocam,
                                contentDescription = null,
                                tint = PulseCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${folder.videoCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PulseCyan
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class FolderFilter { ALL, AUDIO, VIDEO }

@Composable
fun FolderDetailScreen(
    folderName: String,
    folderPath: String,
    items: List<LocalMediaItem>,
    currentPlayingUri: String?,
    isPlaying: Boolean,
    favoriteUris: Set<String>,
    onBack: () -> Unit,
    onPlayItem: (LocalMediaItem, List<LocalMediaItem>, Boolean) -> Unit,
    onToggleFavorite: (LocalMediaItem) -> Unit,
    onAddToPlaylist: (LocalMediaItem) -> Unit
) {
    BackHandler(onBack = onBack)
    var filter by remember { mutableStateOf(FolderFilter.ALL) }

    val visibleItems = remember(items, filter) {
        when (filter) {
            FolderFilter.ALL -> items
            FolderFilter.AUDIO -> items.filter { it.mediaType == MediaType.AUDIO }
            FolderFilter.VIDEO -> items.filter { it.mediaType == MediaType.VIDEO }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("folder_detail_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = folderPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filter == FolderFilter.ALL,
                    onClick = { filter = FolderFilter.ALL },
                    label = { Text("${stringResource(R.string.filter_all)} (${items.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            item {
                val audioCount = items.count { it.mediaType == MediaType.AUDIO }
                FilterChip(
                    selected = filter == FolderFilter.AUDIO,
                    onClick = { filter = FolderFilter.AUDIO },
                    label = { Text("${stringResource(R.string.tab_audio)} ($audioCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            item {
                val videoCount = items.count { it.mediaType == MediaType.VIDEO }
                FilterChip(
                    selected = filter == FolderFilter.VIDEO,
                    onClick = { filter = FolderFilter.VIDEO },
                    label = { Text("${stringResource(R.string.tab_video)} ($videoCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(visibleItems, key = { it.uri }) { item ->
                val isCurrent = item.uri == currentPlayingUri
                val isFav = item.uri in favoriteUris
                if (item.mediaType == MediaType.VIDEO) {
                    VideoItemCard(
                        video = item,
                        isCurrent = isCurrent,
                        isFavorite = isFav,
                        onClick = { onPlayItem(item, visibleItems, false) },
                        onPlayAsAudio = { onPlayItem(item, visibleItems, true) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onAddToPlaylist = { onAddToPlaylist(item) }
                    )
                } else {
                    AudioTrackCard(
                        track = item,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        isFavorite = isFav,
                        onClick = { onPlayItem(item, visibleItems, false) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onAddToPlaylist = { onAddToPlaylist(item) }
                    )
                }
            }
        }
    }
}
