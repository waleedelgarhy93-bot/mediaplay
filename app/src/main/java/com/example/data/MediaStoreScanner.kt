package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    suspend fun scanAudioFiles(): List<LocalMediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<LocalMediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Audio.Media.DATA)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                } else -1
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1
                @Suppress("DEPRECATION")
                val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Audio #$id"
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") "" else rawArtist
                    val album = cursor.getString(albumCol).orEmpty()
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val size = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val mime = cursor.getString(mimeCol).orEmpty()

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val albumArtUri = if (albumId > 0L) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else null

                    val (folderPath, folderName) = extractFolderInfo(
                        relPath = if (relPathCol >= 0) cursor.getString(relPathCol) else null,
                        bucketName = if (bucketCol >= 0) cursor.getString(bucketCol) else null,
                        legacyDataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    )

                    items.add(
                        LocalMediaItem(
                            id = id,
                            uri = contentUri.toString(),
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            dateAddedSec = dateAdded,
                            sizeBytes = size,
                            folderPath = folderPath,
                            folderName = folderName,
                            mediaType = MediaType.AUDIO,
                            albumArtUri = albumArtUri,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted yet
        }
        items
    }

    suspend fun scanVideoFiles(): List<LocalMediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<LocalMediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.TITLE)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.DATE_ADDED)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.WIDTH)
            add(MediaStore.Video.Media.HEIGHT)
            add(MediaStore.Video.Media.MIME_TYPE)
            add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Video.Media.DATA)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Video.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else -1
                @Suppress("DEPRECATION")
                val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Video #$id"
                    val duration = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val size = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val mime = cursor.getString(mimeCol).orEmpty()

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val (folderPath, folderName) = extractFolderInfo(
                        relPath = if (relPathCol >= 0) cursor.getString(relPathCol) else null,
                        bucketName = if (bucketCol >= 0) cursor.getString(bucketCol) else null,
                        legacyDataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    )

                    items.add(
                        LocalMediaItem(
                            id = id,
                            uri = contentUri.toString(),
                            title = title,
                            artist = folderName,
                            album = "",
                            durationMs = duration,
                            dateAddedSec = dateAdded,
                            sizeBytes = size,
                            folderPath = folderPath,
                            folderName = folderName,
                            mediaType = MediaType.VIDEO,
                            albumArtUri = contentUri.toString(),
                            width = width,
                            height = height,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted yet
        }
        items
    }

    private fun extractFolderInfo(
        relPath: String?,
        bucketName: String?,
        legacyDataPath: String?
    ): Pair<String, String> {
        val cleanedRel = relPath?.trim('/')
        if (!cleanedRel.isNullOrBlank()) {
            val name = bucketName?.takeIf { it.isNotBlank() }
                ?: cleanedRel.substringAfterLast('/')
            return cleanedRel to name
        }
        if (!legacyDataPath.isNullOrBlank()) {
            val parent = File(legacyDataPath).parentFile
            val path = parent?.absolutePath ?: "Internal Storage"
            val name = bucketName?.takeIf { it.isNotBlank() } ?: parent?.name ?: "Internal Storage"
            return path to name
        }
        val fallback = bucketName?.takeIf { it.isNotBlank() } ?: "Internal Storage"
        return fallback to fallback
    }
}
