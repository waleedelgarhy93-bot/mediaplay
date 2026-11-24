package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "mediaUri"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"])]
)
data class PlaylistItemEntity(
    val playlistId: Long,
    val mediaUri: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaUri: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val mediaUri: String,
    val lastPositionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long = System.currentTimeMillis()
)

/** Media the user removed from the app only (file stays on the phone). */
@Entity(tableName = "hidden_media")
data class HiddenMediaEntity(
    @PrimaryKey val mediaUri: String,
    val hiddenAt: Long = System.currentTimeMillis()
)

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val itemCount: Int
)
