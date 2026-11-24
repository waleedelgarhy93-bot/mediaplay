package com.example.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.SharedLinkBus
import com.example.media.MediaFiles
import com.example.ui.components.ConvertDialog
import com.example.ui.components.LocalMediaToolsLauncher
import com.example.ui.components.MediaToolsSheet
import com.example.ui.screens.DownloadScreen
import com.example.viewmodel.ToolsViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaFolder
import com.example.data.model.MediaType
import com.example.ui.components.AddToPlaylistBottomSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.AudioTabScreen
import com.example.ui.screens.FolderDetailScreen
import com.example.ui.screens.FoldersTabScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PermissionScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistSelection
import com.example.ui.screens.PlaylistsTabScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.VideoTabScreen
import com.example.ui.theme.PulseCoral
import com.example.viewmodel.MainViewModel

enum class MainTab(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    AUDIO(
        route = "audio_tab",
        labelRes = R.string.tab_audio,
        selectedIcon = Icons.Rounded.MusicNote,
        unselectedIcon = Icons.Outlined.MusicNote
    ),
    VIDEO(
        route = "video_tab",
        labelRes = R.string.tab_video,
        selectedIcon = Icons.Rounded.Videocam,
        unselectedIcon = Icons.Outlined.Videocam
    ),
    PLAYLISTS(
        route = "playlists_tab",
        labelRes = R.string.tab_playlists,
        selectedIcon = Icons.AutoMirrored.Rounded.QueueMusic,
        unselectedIcon = Icons.AutoMirrored.Outlined.QueueMusic
    ),
    FOLDERS(
        route = "folders_tab",
        labelRes = R.string.tab_folders,
        selectedIcon = Icons.Rounded.Folder,
        unselectedIcon = Icons.Outlined.Folder
    ),
    DOWNLOAD(
        route = "download_tab",
        labelRes = R.string.tab_download,
        selectedIcon = Icons.Rounded.Download,
        unselectedIcon = Icons.Outlined.Download
    )
}

@Composable
fun PulsePlayApp(viewModel: MainViewModel) {
    val hasPermissions by viewModel.hasPermissions.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onPermissionsUpdated()
    }

    if (!hasPermissions) {
        PermissionScreen(
            onPermissionsResult = { viewModel.onPermissionsUpdated() }
        )
        return
    }

    val toolsViewModel: ToolsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    SideEffect { toolsViewModel.onLibraryChanged = { viewModel.refreshLibrary() } }
    val hiddenUris by viewModel.hiddenUris.collectAsStateWithLifecycle()
    val convertJob by toolsViewModel.convertJob.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val audioTracks by viewModel.filteredAudio.collectAsStateWithLifecycle()
    val audioSearch by viewModel.audioSearchQuery.collectAsStateWithLifecycle()
    val audioSort by viewModel.audioSortOption.collectAsStateWithLifecycle()
    val videos by viewModel.filteredVideos.collectAsStateWithLifecycle()
    val videoSearch by viewModel.videoSearchQuery.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val customPlaylists by viewModel.customPlaylists.collectAsStateWithLifecycle()
    val favoriteUris by viewModel.favoriteUris.collectAsStateWithLifecycle()
    val favoriteItems by viewModel.favoriteItems.collectAsStateWithLifecycle()
    val recentItems by viewModel.recentlyPlayedItems.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val mediaController by viewModel.mediaController.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.AUDIO) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistSelection?>(null) }
    var selectedFolder by remember { mutableStateOf<MediaFolder?>(null) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var isVideoPlayerOpen by remember { mutableStateOf(false) }
    var playlistPickerTarget by remember { mutableStateOf<LocalMediaItem?>(null) }
    var toolsTarget by remember { mutableStateOf<LocalMediaItem?>(null) }
    var convertTarget by remember { mutableStateOf<LocalMediaItem?>(null) }
    var pendingDelete by remember { mutableStateOf<Pair<LocalMediaItem, Boolean>?>(null) }

    // Link shared from YouTube/Facebook/... -> open the Download tab with it
    LaunchedEffect(Unit) {
        SharedLinkBus.pending.collect { link ->
            if (link != null) {
                toolsViewModel.setUrl(link)
                currentTab = MainTab.DOWNLOAD
                selectedPlaylist = null
                selectedFolder = null
                SharedLinkBus.pending.value = null
            }
        }
    }

    val launchDeleteRequest = remember { mutableStateOf<(MediaFiles.DeleteResult.NeedsConfirmation) -> Unit>({}) }

    fun deleteFromPhone(item: LocalMediaItem) {
        scope.launch {
            when (val result = MediaFiles.delete(context, android.net.Uri.parse(item.uri))) {
                MediaFiles.DeleteResult.Deleted -> {
                    viewModel.onDeletedFromDevice(item)
                    Toast.makeText(context, R.string.deleted_ok, Toast.LENGTH_SHORT).show()
                }
                is MediaFiles.DeleteResult.NeedsConfirmation -> {
                    pendingDelete = item to result.deletesItself
                    launchDeleteRequest.value(result)
                }
                is MediaFiles.DeleteResult.Failed -> Toast.makeText(
                    context,
                    context.getString(R.string.delete_failed, result.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pending = pendingDelete
        pendingDelete = null
        if (result.resultCode == Activity.RESULT_OK && pending != null) {
            val (item, deletesItself) = pending
            if (deletesItself) {
                viewModel.onDeletedFromDevice(item)
                Toast.makeText(context, R.string.deleted_ok, Toast.LENGTH_SHORT).show()
            } else {
                // Android 10: access granted, now delete for real
                deleteFromPhone(item)
            }
        }
    }
    launchDeleteRequest.value = { request ->
        deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
    }

    val handlePlayMedia: (LocalMediaItem, List<LocalMediaItem>, Boolean) -> Unit =
        { item, queue, asAudioOnly ->
            viewModel.playItem(item = item, queue = queue, asAudioOnly = asAudioOnly)
            if (item.mediaType == MediaType.VIDEO && !asAudioOnly) {
                isNowPlayingExpanded = false
                isVideoPlayerOpen = true
            } else {
                isVideoPlayerOpen = false
            }
        }

    CompositionLocalProvider(LocalMediaToolsLauncher provides { item -> toolsTarget = item }) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            if (useNavigationRail) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    currentTab = tab
                                    selectedPlaylist = null
                                    selectedFolder = null
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PulseCoral,
                                    selectedTextColor = PulseCoral,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        MainTabContent(
                            currentTab = currentTab,
                            selectedPlaylist = selectedPlaylist,
                            selectedFolder = selectedFolder,
                            viewModel = viewModel,
                            audioTracks = audioTracks,
                            audioSearch = audioSearch,
                            audioSort = audioSort,
                            videos = videos,
                            videoSearch = videoSearch,
                            folders = folders,
                            customPlaylists = customPlaylists,
                            favoriteUris = favoriteUris,
                            favoriteItems = favoriteItems,
                            recentItems = recentItems,
                            isScanning = isScanning,
                            playbackState = playbackState,
                            onSelectPlaylist = { selectedPlaylist = it },
                            onSelectFolder = { selectedFolder = it },
                            onPlayMedia = handlePlayMedia,
                            onAddToPlaylist = { playlistPickerTarget = it },
                            toolsViewModel = toolsViewModel,
                            hiddenCount = hiddenUris.size
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            visible = playbackState.currentMedia != null,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            MiniPlayerBar(
                                playbackState = playbackState,
                                onTogglePlayPause = { viewModel.playbackManager.togglePlayPause() },
                                onNext = { viewModel.playbackManager.skipToNext() },
                                onExpand = {
                                    val current = playbackState.currentMedia
                                    if (current?.mediaType == MediaType.VIDEO && !playbackState.isAudioOnlyVideoMode) {
                                        isVideoPlayerOpen = true
                                    } else {
                                        isNowPlayingExpanded = true
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.statusBars,
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            AnimatedVisibility(
                                visible = playbackState.currentMedia != null,
                                enter = slideInVertically { it } + fadeIn(),
                                exit = slideOutVertically { it } + fadeOut()
                            ) {
                                MiniPlayerBar(
                                    playbackState = playbackState,
                                    onTogglePlayPause = { viewModel.playbackManager.togglePlayPause() },
                                    onNext = { viewModel.playbackManager.skipToNext() },
                                    onExpand = {
                                        val current = playbackState.currentMedia
                                        if (current?.mediaType == MediaType.VIDEO && !playbackState.isAudioOnlyVideoMode) {
                                            isVideoPlayerOpen = true
                                        } else {
                                            isNowPlayingExpanded = true
                                        }
                                    }
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                thickness = 0.5.dp
                            )

                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            currentTab = tab
                                            selectedPlaylist = null
                                            selectedFolder = null
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(tab.labelRes),
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PulseCoral,
                                            selectedTextColor = PulseCoral,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_${tab.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MainTabContent(
                            currentTab = currentTab,
                            selectedPlaylist = selectedPlaylist,
                            selectedFolder = selectedFolder,
                            viewModel = viewModel,
                            audioTracks = audioTracks,
                            audioSearch = audioSearch,
                            audioSort = audioSort,
                            videos = videos,
                            videoSearch = videoSearch,
                            folders = folders,
                            customPlaylists = customPlaylists,
                            favoriteUris = favoriteUris,
                            favoriteItems = favoriteItems,
                            recentItems = recentItems,
                            isScanning = isScanning,
                            playbackState = playbackState,
                            onSelectPlaylist = { selectedPlaylist = it },
                            onSelectFolder = { selectedFolder = it },
                            onPlayMedia = handlePlayMedia,
                            onAddToPlaylist = { playlistPickerTarget = it },
                            toolsViewModel = toolsViewModel,
                            hiddenCount = hiddenUris.size
                        )
                    }
                }
            }
        }

        // Full Now Playing Sheet Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded && playbackState.currentMedia != null && !isVideoPlayerOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val currentUri = playbackState.currentMedia?.uri
            NowPlayingScreen(
                playbackState = playbackState,
                isFavorite = currentUri != null && currentUri in favoriteUris,
                onCollapse = { isNowPlayingExpanded = false },
                onTogglePlayPause = { viewModel.playbackManager.togglePlayPause() },
                onPrevious = { viewModel.playbackManager.skipToPrevious() },
                onNext = { viewModel.playbackManager.skipToNext() },
                onSeekTo = { viewModel.playbackManager.seekTo(it) },
                onCycleRepeat = { viewModel.playbackManager.cycleRepeatMode() },
                onToggleShuffle = { viewModel.playbackManager.toggleShuffle() },
                onSelectSpeed = { viewModel.playbackManager.setPlaybackSpeed(it) },
                onSelectSleepTimer = { viewModel.setSleepTimer(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                onAddToPlaylist = { playlistPickerTarget = it },
                onSwitchToVideo = {
                    viewModel.playbackManager.setAudioOnlyMode(false)
                    isNowPlayingExpanded = false
                    isVideoPlayerOpen = true
                }
            )
        }

        // Full-Screen Video Player Overlay
        AnimatedVisibility(
            visible = isVideoPlayerOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val currentUri = playbackState.currentMedia?.uri
            VideoPlayerScreen(
                mediaController = mediaController,
                playbackState = playbackState,
                isFavorite = currentUri != null && currentUri in favoriteUris,
                onClose = {
                    isVideoPlayerOpen = false
                    if (playbackState.isPlaying) {
                        viewModel.playbackManager.togglePlayPause()
                    }
                },
                onPlayAsAudio = {
                    viewModel.playbackManager.setAudioOnlyMode(true)
                    isVideoPlayerOpen = false
                },
                onTogglePlayPause = { viewModel.playbackManager.togglePlayPause() },
                onSeekTo = { viewModel.playbackManager.seekTo(it) },
                onSeekBy = { viewModel.playbackManager.seekBy(it) },
                onSelectSpeed = { viewModel.playbackManager.setPlaybackSpeed(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                onAddToPlaylist = { playlistPickerTarget = it }
            )
        }

        // Add to Playlist Modal Bottom Sheet
        playlistPickerTarget?.let { targetItem ->
            val selectedIds by remember(targetItem.uri) {
                viewModel.observePlaylistIdsForMedia(targetItem.uri)
            }.collectAsStateWithLifecycle(initialValue = emptySet())

            AddToPlaylistBottomSheet(
                mediaItem = targetItem,
                playlists = customPlaylists,
                selectedPlaylistIds = selectedIds,
                onTogglePlaylist = { playlistId, currentlyIn ->
                    viewModel.toggleMediaInPlaylist(playlistId, targetItem.uri, currentlyIn)
                },
                onCreatePlaylist = { name ->
                    viewModel.createPlaylist(name, initialMediaUri = targetItem.uri)
                },
                onDismiss = { playlistPickerTarget = null }
            )
        }

        // Cut / Convert / Delete sheet
        toolsTarget?.let { item ->
            MediaToolsSheet(
                item = item,
                onConvert = {
                    toolsTarget = null
                    toolsViewModel.dismissConvertJob()
                    convertTarget = item
                },
                onHide = {
                    toolsTarget = null
                    viewModel.hideFromApp(item)
                    Toast.makeText(context, R.string.hidden_ok, Toast.LENGTH_SHORT).show()
                },
                onDeleteFromPhone = {
                    toolsTarget = null
                    deleteFromPhone(item)
                },
                onDismiss = { toolsTarget = null }
            )
        }

        convertTarget?.let { item ->
            ConvertDialog(
                item = item,
                job = convertJob,
                onStart = { toolsViewModel.startConvert(it) },
                onCancelJob = { toolsViewModel.cancelConvert() },
                onDismiss = {
                    convertTarget = null
                    toolsViewModel.dismissConvertJob()
                }
            )
        }
    }
    }
}

@Composable
private fun MainTabContent(
    currentTab: MainTab,
    selectedPlaylist: PlaylistSelection?,
    selectedFolder: MediaFolder?,
    viewModel: MainViewModel,
    audioTracks: List<LocalMediaItem>,
    audioSearch: String,
    audioSort: com.example.data.model.AudioSortOption,
    videos: List<LocalMediaItem>,
    videoSearch: String,
    folders: List<MediaFolder>,
    customPlaylists: List<com.example.data.local.PlaylistWithCount>,
    favoriteUris: Set<String>,
    favoriteItems: List<LocalMediaItem>,
    recentItems: List<LocalMediaItem>,
    isScanning: Boolean,
    playbackState: com.example.player.PlaybackUiState,
    onSelectPlaylist: (PlaylistSelection?) -> Unit,
    onSelectFolder: (MediaFolder?) -> Unit,
    onPlayMedia: (LocalMediaItem, List<LocalMediaItem>, Boolean) -> Unit,
    onAddToPlaylist: (LocalMediaItem) -> Unit,
    toolsViewModel: ToolsViewModel,
    hiddenCount: Int
) {
    val currentUri = playbackState.currentMedia?.uri

    when {
        currentTab == MainTab.PLAYLISTS && selectedPlaylist != null -> {
            when (selectedPlaylist) {
                PlaylistSelection.Favorites -> {
                    PlaylistDetailScreen(
                        title = stringResource(R.string.favorites_title),
                        items = favoriteItems,
                        canRemoveItems = false,
                        currentPlayingUri = currentUri,
                        isPlaying = playbackState.isPlaying,
                        favoriteUris = favoriteUris,
                        onBack = { onSelectPlaylist(null) },
                        onPlayItem = onPlayMedia,
                        onShuffleAll = { viewModel.shuffleAndPlay(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                        onAddToPlaylist = onAddToPlaylist
                    )
                }

                PlaylistSelection.RecentlyPlayed -> {
                    PlaylistDetailScreen(
                        title = stringResource(R.string.recently_played_title),
                        items = recentItems,
                        canRemoveItems = false,
                        currentPlayingUri = currentUri,
                        isPlaying = playbackState.isPlaying,
                        favoriteUris = favoriteUris,
                        onBack = { onSelectPlaylist(null) },
                        onPlayItem = onPlayMedia,
                        onShuffleAll = { viewModel.shuffleAndPlay(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                        onAddToPlaylist = onAddToPlaylist
                    )
                }

                is PlaylistSelection.Custom -> {
                    val items by remember(selectedPlaylist.id) {
                        viewModel.observePlaylistItems(selectedPlaylist.id)
                    }.collectAsStateWithLifecycle(initialValue = emptyList())

                    PlaylistDetailScreen(
                        title = selectedPlaylist.name,
                        items = items,
                        canRemoveItems = true,
                        currentPlayingUri = currentUri,
                        isPlaying = playbackState.isPlaying,
                        favoriteUris = favoriteUris,
                        onBack = { onSelectPlaylist(null) },
                        onPlayItem = onPlayMedia,
                        onShuffleAll = { viewModel.shuffleAndPlay(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                        onAddToPlaylist = onAddToPlaylist,
                        onRemoveFromPlaylist = { item ->
                            viewModel.removeMediaFromPlaylist(selectedPlaylist.id, item.uri)
                        }
                    )
                }
            }
        }

        currentTab == MainTab.FOLDERS && selectedFolder != null -> {
            val folderItems by remember(selectedFolder.path) {
                viewModel.getItemsForFolder(selectedFolder.path)
            }.collectAsStateWithLifecycle(initialValue = emptyList())

            FolderDetailScreen(
                folderName = selectedFolder.name,
                folderPath = selectedFolder.path,
                items = folderItems,
                currentPlayingUri = currentUri,
                isPlaying = playbackState.isPlaying,
                favoriteUris = favoriteUris,
                onBack = { onSelectFolder(null) },
                onPlayItem = onPlayMedia,
                onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                onAddToPlaylist = onAddToPlaylist
            )
        }

        else -> {
            when (currentTab) {
                MainTab.AUDIO -> {
                    AudioTabScreen(
                        tracks = audioTracks,
                        searchQuery = audioSearch,
                        sortOption = audioSort,
                        isScanning = isScanning,
                        currentPlayingUri = currentUri,
                        isPlaying = playbackState.isPlaying,
                        favoriteUris = favoriteUris,
                        onSearchChange = { viewModel.updateAudioSearch(it) },
                        onSortChange = { viewModel.updateAudioSort(it) },
                        onRefresh = { viewModel.refreshLibrary() },
                        onTrackClick = { track, queue -> onPlayMedia(track, queue, false) },
                        onShuffleAll = { viewModel.shuffleAndPlay(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                        onAddToPlaylist = onAddToPlaylist
                    )
                }

                MainTab.VIDEO -> {
                    VideoTabScreen(
                        videos = videos,
                        searchQuery = videoSearch,
                        isScanning = isScanning,
                        currentPlayingUri = currentUri,
                        favoriteUris = favoriteUris,
                        onSearchChange = { viewModel.updateVideoSearch(it) },
                        onRefresh = { viewModel.refreshLibrary() },
                        onVideoClick = { video, queue -> onPlayMedia(video, queue, false) },
                        onPlayAsAudio = { video, queue -> onPlayMedia(video, queue, true) },
                        onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                        onAddToPlaylist = onAddToPlaylist
                    )
                }

                MainTab.PLAYLISTS -> {
                    PlaylistsTabScreen(
                        playlists = customPlaylists,
                        favoritesCount = favoriteItems.size,
                        recentCount = recentItems.size,
                        onOpenFavorites = { onSelectPlaylist(PlaylistSelection.Favorites) },
                        onOpenRecent = { onSelectPlaylist(PlaylistSelection.RecentlyPlayed) },
                        onOpenPlaylist = { playlist ->
                            onSelectPlaylist(PlaylistSelection.Custom(playlist.id, playlist.name))
                        },
                        onCreatePlaylist = { viewModel.createPlaylist(it) },
                        onRenamePlaylist = { id, newName -> viewModel.renamePlaylist(id, newName) },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) }
                    )
                }

                MainTab.FOLDERS -> {
                    FoldersTabScreen(
                        folders = folders,
                        onRefresh = { viewModel.refreshLibrary() },
                        onFolderClick = { onSelectFolder(it) }
                    )
                }

                MainTab.DOWNLOAD -> {
                    val url by toolsViewModel.urlInput.collectAsStateWithLifecycle()
                    val tasks by toolsViewModel.downloads.collectAsStateWithLifecycle()
                    val engineStatus by toolsViewModel.engineStatus.collectAsStateWithLifecycle()
                    DownloadScreen(
                        url = url,
                        tasks = tasks,
                        engineStatus = engineStatus,
                        hiddenCount = hiddenCount,
                        onUrlChange = { toolsViewModel.setUrl(it) },
                        onDownload = { kind, quality, bitrate -> toolsViewModel.startDownload(kind, quality, bitrate) },
                        onCancel = { toolsViewModel.cancelDownload(it) },
                        onClearFinished = { toolsViewModel.clearFinished() },
                        onUpdateEngine = { toolsViewModel.updateEngine() },
                        onRestoreHidden = { viewModel.unhideAll() }
                    )
                }
            }
        }
    }
}
