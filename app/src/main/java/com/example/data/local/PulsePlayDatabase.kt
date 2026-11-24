package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        FavoriteEntity::class,
        PlaybackHistoryEntity::class,
        HiddenMediaEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PulsePlayDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: PulsePlayDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `hidden_media` (" +
                        "`mediaUri` TEXT NOT NULL, `hiddenAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`mediaUri`))"
                )
            }
        }

        fun getInstance(context: Context): PulsePlayDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PulsePlayDatabase::class.java,
                    "pulseplay_local.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }
    }
}
