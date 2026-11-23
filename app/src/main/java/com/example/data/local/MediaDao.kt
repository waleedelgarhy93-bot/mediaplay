package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    // Playlists
    @Query(
        """
        SELECT p.id, p.name, p.createdAt, COUNT(pi.mediaUri) AS itemCount
        FROM playlists p
        LEFT JOIN playlist_items pi ON p.id = pi.playlistId
        GROUP BY p.id
        ORDER BY p.createdAt DESC
        """
    )
    fun observePlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    // Playlist Items
    @Query("SELECT mediaUri FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun observePlaylistItemUris(playlistId: Long): Flow<List<String>>

    @Query("SELECT playlistId FROM playlist_items WHERE mediaUri = :mediaUri")
    fun observePlaylistIdsForMedia(mediaUri: String): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItemToPlaylist(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaUri = :mediaUri")
    suspend fun removeItemFromPlaylist(playlistId: Long, mediaUri: String)

    // Favorites
    @Query("SELECT mediaUri FROM favorites ORDER BY addedAt DESC")
    fun observeFavoriteUris(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaUri = :mediaUri")
    suspend fun removeFavorite(mediaUri: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaUri = :mediaUri)")
    suspend fun isFavorite(mediaUri: String): Boolean

    // Playback History & Last-Played Positions
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT 50")
    fun observeRecentHistory(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE mediaUri = :mediaUri LIMIT 1")
    suspend fun getPlaybackHistoryForUri(mediaUri: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaybackHistory(history: PlaybackHistoryEntity)
}
