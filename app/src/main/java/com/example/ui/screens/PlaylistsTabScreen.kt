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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PlaylistWithCount
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaType
import com.example.ui.components.EmptyLibraryState
import com.example.ui.components.PlaylistNameDialog
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseCyan

sealed class PlaylistSelection {
    data object Favorites : PlaylistSelection()
    data object RecentlyPlayed : PlaylistSelection()
    data class Custom(val id: Long, val name: String) : PlaylistSelection()
}

@Composable
fun PlaylistsTabScreen(
    playlists: List<PlaylistWithCount>,
    favoritesCount: Int,
    recentCount: Int,
    onOpenFavorites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenPlaylist: (PlaylistWithCount) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var deletingPlaylist by remember { mutableStateOf<PlaylistWithCount?>(null) }

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
            Text(
                text = stringResource(R.string.playlists_header),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            FilledTonalButton(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("create_playlist_button")
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 6.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Auto-Generated Smart Lists (Favorites & Recently Played)
            item(key = "smart_lists_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmartPlaylistCard(
                        title = stringResource(R.string.favorites_title),
                        countText = "$favoritesCount items",
                        icon = Icons.Rounded.Favorite,
                        accentColor = PulseCoral,
                        onClick = onOpenFavorites,
                        testTag = "favorites_playlist_card",
                        modifier = Modifier.weight(1f)
                    )
                    SmartPlaylistCard(
                        title = stringResource(R.string.recently_played_title),
                        countText = "$recentCount items",
                        icon = Icons.Rounded.History,
                        accentColor = PulseCyan,
                        onClick = onOpenRecent,
                        testTag = "recent_playlist_card",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "custom_header") {
                Text(
                    text = stringResource(R.string.custom_playlists_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }

            if (playlists.isEmpty()) {
                item(key = "empty_custom") {
                    EmptyLibraryState(
                        title = stringResource(R.string.empty_playlists_title),
                        description = stringResource(R.string.empty_playlists_desc)
                    )
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    CustomPlaylistRow(
                        playlist = playlist,
                        onClick = { onOpenPlaylist(playlist) },
                        onRename = { renamingPlaylist = playlist },
                        onDelete = { deletingPlaylist = playlist }
                    )
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

    renamingPlaylist?.let { target ->
        PlaylistNameDialog(
            title = stringResource(R.string.rename_playlist),
            initialName = target.name,
            confirmLabel = stringResource(R.string.save),
            onConfirm = { newName ->
                onRenamePlaylist(target.id, newName)
                renamingPlaylist = null
            },
            onDismiss = { renamingPlaylist = null }
        )
    }

    deletingPlaylist?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingPlaylist = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.delete_playlist)) },
            text = { Text("Delete \"${target.name}\"? Your local files will not be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlaylist(target.id)
                        deletingPlaylist = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRose,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_playlist")
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlaylist = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SmartPlaylistCard(
    title: String,
    countText: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(124.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CustomPlaylistRow(
    playlist: PlaylistWithCount,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("custom_playlist_card_${playlist.id}"),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = playlist.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.itemCount} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        text = { Text(stringResource(R.string.rename_playlist)) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_playlist), color = ErrorRose) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = ErrorRose)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    title: String,
    items: List<LocalMediaItem>,
    canRemoveItems: Boolean,
    currentPlayingUri: String?,
    isPlaying: Boolean,
    favoriteUris: Set<String>,
    onBack: () -> Unit,
    onPlayItem: (LocalMediaItem, List<LocalMediaItem>, Boolean) -> Unit,
    onShuffleAll: (List<LocalMediaItem>) -> Unit,
    onToggleFavorite: (LocalMediaItem) -> Unit,
    onAddToPlaylist: (LocalMediaItem) -> Unit,
    onRemoveFromPlaylist: ((LocalMediaItem) -> Unit)? = null
) {
    BackHandler(onBack = onBack)

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
                    .testTag("playlist_detail_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${items.size} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (items.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onPlayItem(items.first(), items, false) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("playlist_play_all_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All")
                }
                FilledTonalButton(
                    onClick = { onShuffleAll(items) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("playlist_shuffle_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.shuffle))
                }
            }
        }

        if (items.isEmpty()) {
            EmptyLibraryState(
                title = title,
                description = stringResource(R.string.empty_playlist_items)
            )
        } else {
            val removeLabel = stringResource(R.string.remove_from_playlist)
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
                items(items, key = { it.uri }) { item ->
                    val isCurrent = item.uri == currentPlayingUri
                    val isFav = item.uri in favoriteUris
                    if (item.mediaType == MediaType.VIDEO) {
                        VideoItemCard(
                            video = item,
                            isCurrent = isCurrent,
                            isFavorite = isFav,
                            onClick = { onPlayItem(item, items, false) },
                            onPlayAsAudio = { onPlayItem(item, items, true) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onAddToPlaylist = { onAddToPlaylist(item) },
                            extraMenuActionLabel = if (canRemoveItems) removeLabel else null,
                            onExtraMenuAction = if (canRemoveItems) {
                                { onRemoveFromPlaylist?.invoke(item) }
                            } else null
                        )
                    } else {
                        AudioTrackCard(
                            track = item,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            isFavorite = isFav,
                            onClick = { onPlayItem(item, items, false) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onAddToPlaylist = { onAddToPlaylist(item) },
                            extraMenuActionLabel = if (canRemoveItems) removeLabel else null,
                            onExtraMenuAction = if (canRemoveItems) {
                                { onRemoveFromPlaylist?.invoke(item) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
