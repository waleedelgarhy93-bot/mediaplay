package com.example.media

import android.content.Context
import android.net.Uri
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File

enum class DownloadKind { VIDEO, AUDIO }

/** Max video height for downloads; null = best available. */
enum class DownloadQuality(val height: Int?, val label: String) {
    BEST(null, ""),
    P1080(1080, "1080p"),
    P720(720, "720p"),
    P480(480, "480p"),
    P360(360, "360p"),
    P240(240, "240p")
}

/**
 * Downloads media from YouTube, Facebook, Instagram, TikTok, X and ~1000 other sites
 * using yt-dlp, then saves it into Movies/ or Music/ "Lopital Media".
 */
object MediaDownloader {

    @Volatile
    private var updatedThisSession = false

    /** Updates the yt-dlp engine (sites change often). Returns a short status text. */
    suspend fun updateEngine(context: Context): String = withContext(Dispatchers.IO) {
        MediaEngine.ensureInit(context)
        val status = YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext)
        updatedThisSession = true
        val version = YoutubeDL.getInstance().version(context.applicationContext).orEmpty()
        "${status?.toString() ?: "OK"} $version".trim()
    }

    private fun formatSelector(kind: DownloadKind, quality: DownloadQuality): String {
        if (kind == DownloadKind.AUDIO) return "bestaudio/best"
        val h = quality.height
        return if (h == null) {
            "bv*[ext=mp4]+ba[ext=m4a]/bv*+ba/b"
        } else {
            "bv*[height<=$h][ext=mp4]+ba[ext=m4a]/bv*[height<=$h]+ba/b[height<=$h]/b"
        }
    }

    /**
     * Runs the download. [onProgress] gets 0..1 (or -1 when unknown) and the last log line.
     * Returns the Uris of the saved files.
     */
    suspend fun download(
        context: Context,
        url: String,
        kind: DownloadKind,
        quality: DownloadQuality,
        audioBitrate: AudioBitrate,
        processId: String,
        onProgress: (Float, String) -> Unit
    ): List<Uri> = withContext(Dispatchers.IO) {
        MediaEngine.ensureInit(context)
        if (!updatedThisSession) {
            // Keep yt-dlp fresh so YouTube & co. keep working; ignore failures (offline etc.).
            onProgress(-1f, "Updating download engine…")
            runCatching { updateEngine(context) }
            updatedThisSession = true
        }

        val outDir = File(context.cacheDir, "downloads/$processId").apply {
            deleteRecursively(); mkdirs()
        }
        val request = YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("--no-part")
            addOption("-o", outDir.absolutePath + "/%(title).80B [%(id)s].%(ext)s")
            addOption("-f", formatSelector(kind, quality))
            if (kind == DownloadKind.AUDIO) {
                addOption("-x")
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", "${audioBitrate.kbps}K")
            } else {
                addOption("--merge-output-format", "mp4")
            }
        }

        try {
            runInterruptible {
                YoutubeDL.getInstance().execute(request, processId) { progress, _, line ->
                    onProgress(if (progress >= 0f) progress / 100f else -1f, line)
                }
            }
        } finally {
            YoutubeDL.getInstance().destroyProcessById(processId)
        }

        val files = outDir.listFiles()
            ?.filter { it.isFile && it.length() > 0 && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") }
            .orEmpty()
        if (files.isEmpty()) throw IllegalStateException("No file was downloaded")
        onProgress(1f, "Saving…")
        val saved = files.mapNotNull { file ->
            val title = file.nameWithoutExtension.replace(Regex("""\s*\[[^\]]+]$"""), "")
            MediaFiles.publish(context, file, MediaFiles.safeFileName(title))
        }
        outDir.deleteRecursively()
        saved
    }

    fun cancel(processId: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
    }
}
