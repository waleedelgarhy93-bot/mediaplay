package com.example.media

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Folder name used under Movies/ and Music/ for everything the app creates. */
const val APP_MEDIA_FOLDER = "Lopital Media"

object MediaFiles {

    fun mimeForExtension(ext: String): String = when (ext.lowercase()) {
        "mp4" -> "video/mp4"
        "3gp" -> "video/3gpp"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "opus" -> "audio/ogg"
        "ogg" -> "audio/ogg"
        "wav" -> "audio/x-wav"
        "flac" -> "audio/flac"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
            ?: "application/octet-stream"
    }

    fun isVideoExtension(ext: String): Boolean = mimeForExtension(ext).startsWith("video/")

    /** Copies a content:// item into the cache so the ffmpeg process can read it. */
    suspend fun copyToCache(context: Context, uri: Uri, name: String): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "work").apply { mkdirs() }
            val out = File(dir, name)
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Cannot open file" }
                out.outputStream().use { input.copyTo(it) }
            }
            out
        }

    fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Exception) {
        null
    }

    fun safeFileName(raw: String): String =
        raw.replace(Regex("""[\\/:*?"<>|\n\r\t]"""), "_").trim().take(90).ifBlank { "media" }

    /**
     * Moves [file] into the public Movies/Lopital Media (video) or Music/Lopital Media (audio)
     * folder so it shows up in the library and in other apps. Deletes [file] afterwards.
     */
    suspend fun publish(context: Context, file: File, displayName: String): Uri? =
        withContext(Dispatchers.IO) {
            val ext = file.extension
            val mime = mimeForExtension(ext)
            val isVideo = mime.startsWith("video/")
            val finalName = if (displayName.endsWith(".$ext", true)) displayName else "$displayName.$ext"
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val collection = if (isVideo) {
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    }
                    val relative = (if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC) +
                        "/" + APP_MEDIA_FOLDER
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mime)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relative)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(collection, values) ?: return@withContext null
                    resolver.openOutputStream(uri).use { out ->
                        requireNotNull(out)
                        file.inputStream().use { it.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    uri
                } else {
                    @Suppress("DEPRECATION")
                    val base = Environment.getExternalStoragePublicDirectory(
                        if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
                    )
                    val dir = File(base, APP_MEDIA_FOLDER).apply { mkdirs() }
                    var target = File(dir, finalName)
                    var n = 1
                    while (target.exists()) {
                        target = File(dir, "${finalName.substringBeforeLast('.')} ($n).$ext"); n++
                    }
                    file.copyTo(target)
                    MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mime), null)
                    Uri.fromFile(target)
                }
            } finally {
                file.delete()
            }
        }

    sealed interface DeleteResult {
        data object Deleted : DeleteResult
        /** The system must ask the user first; launch this, then call [delete] again if needed. */
        data class NeedsConfirmation(val intentSender: IntentSender, val deletesItself: Boolean) : DeleteResult
        data class Failed(val message: String) : DeleteResult
    }

    /** Deletes a MediaStore item from the phone. */
    suspend fun delete(context: Context, uri: Uri): DeleteResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: system dialog performs the delete itself.
                val pending = MediaStore.createDeleteRequest(resolver, listOf(uri))
                return@withContext DeleteResult.NeedsConfirmation(pending.intentSender, deletesItself = true)
            }
            val rows = resolver.delete(uri, null, null)
            if (rows > 0) DeleteResult.Deleted else DeleteResult.Failed("File not found")
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                DeleteResult.NeedsConfirmation(e.userAction.actionIntent.intentSender, deletesItself = false)
            } else {
                DeleteResult.Failed(e.message ?: "Permission denied")
            }
        } catch (e: Exception) {
            DeleteResult.Failed(e.message ?: "Error")
        }
    }
}
