package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        FavoriteEntity::class,
        PlaybackHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PulsePlayDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: PulsePlayDatabase? = null

        fun getInstance(context: Context): PulsePlayDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PulsePlayDatabase::class.java,
                    "pulseplay_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
