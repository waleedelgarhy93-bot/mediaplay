package com.example.data

import com.example.data.local.FavoriteEntity
import com.example.data.local.HiddenMediaEntity
import com.example.data.local.MediaDao
import com.example.data.local.PlaybackHistoryEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistItemEntity
import com.example.data.local.PlaylistWithCount
import com.example.data.model.LocalMediaItem
import kotlinx.coroutines.flow.Flow

class MediaRepository(
    private val scanner: MediaStoreScanner,
    private val dao: MediaDao
) {
    suspend fun scanAudio(): List<LocalMediaItem> = scanner.scanAudioFiles()
    suspend fun scanVideo(): List<LocalMediaItem> = scanner.scanVideoFiles()

    val playlists: Flow<List<PlaylistWithCount>> = dao.observePlaylistsWithCount()
    val favoriteUris: Flow<List<String>> = dao.observeFavoriteUris()
    val recentHistory: Flow<List<PlaybackHistoryEntity>> = dao.observeRecentHistory()
    val hiddenUris: Flow<List<String>> = dao.observeHiddenUris()

    suspend fun hideMedia(mediaUri: String) {
        dao.hideMedia(HiddenMediaEntity(mediaUri = mediaUri))
    }

    suspend fun unhideAll() {
        dao.unhideAll()
    }

    /** Removes every trace of a file that no longer exists on the phone. */
    suspend fun forgetDeletedMedia(mediaUri: String) {
        dao.removeFavorite(mediaUri)
        dao.deletePlaylistItemsForUri(mediaUri)
        dao.deleteHistoryForUri(mediaUri)
    }

    fun observePlaylistItemUris(playlistId: Long): Flow<List<String>> =
        dao.observePlaylistItemUris(playlistId)

    fun observePlaylistIdsForMedia(mediaUri: String): Flow<List<Long>> =
        dao.observePlaylistIdsForMedia(mediaUri)

    suspend fun createPlaylist(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        return dao.insertPlaylist(PlaylistEntity(name = trimmed))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            dao.renamePlaylist(playlistId, trimmed)
        }
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addItemToPlaylist(playlistId: Long, mediaUri: String) {
        dao.addItemToPlaylist(PlaylistItemEntity(playlistId = playlistId, mediaUri = mediaUri))
    }

    suspend fun removeItemFromPlaylist(playlistId: Long, mediaUri: String) {
        dao.removeItemFromPlaylist(playlistId, mediaUri)
    }

    suspend fun toggleFavorite(mediaUri: String): Boolean {
        val exists = dao.isFavorite(mediaUri)
        if (exists) {
            dao.removeFavorite(mediaUri)
            return false
        } else {
            dao.addFavorite(FavoriteEntity(mediaUri = mediaUri))
            return true
        }
    }

    suspend fun savePlaybackPosition(mediaUri: String, positionMs: Long, durationMs: Long) {
        dao.upsertPlaybackHistory(
            PlaybackHistoryEntity(
                mediaUri = mediaUri,
                lastPositionMs = positionMs.coerceAtLeast(0L),
                durationMs = durationMs.coerceAtLeast(0L),
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getLastPlayedPosition(mediaUri: String): Long {
        val history = dao.getPlaybackHistoryForUri(mediaUri) ?: return 0L
        // Resume only if user watched/listened > 5s and hasn't basically finished (>95%)
        if (history.durationMs > 15_000L &&
            history.lastPositionMs in 5_000L..(history.durationMs * 95 / 100)
        ) {
            return history.lastPositionMs
        }
        return 0L
    }
}
