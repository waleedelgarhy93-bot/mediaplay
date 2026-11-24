package com.example.media

import android.content.Context
import android.net.Uri
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaType
import java.io.File

/** Output formats offered by the converter. */
enum class OutputFormat(val extension: String, val isVideo: Boolean, val label: String) {
    MP4("mp4", true, "MP4"),
    THREE_GP("3gp", true, "3GP"),
    MP3("mp3", false, "MP3"),
    M4A("m4a", false, "M4A"),
    /** Cut only, no re-encoding: very fast, keeps the original format. */
    ORIGINAL("", false, "");

    companion object {
        fun choicesFor(type: MediaType): List<OutputFormat> =
            if (type == MediaType.VIDEO) listOf(MP4, THREE_GP, MP3, M4A, ORIGINAL)
            else listOf(MP3, M4A, ORIGINAL)
    }
}

/** Video height limits. ORIGINAL keeps the source resolution. */
enum class VideoQuality(val height: Int?, val label: String) {
    ORIGINAL(null, ""),
    P1080(1080, "1080p"),
    P720(720, "720p"),
    P480(480, "480p"),
    P360(360, "360p"),
    P240(240, "240p"),
    P144(144, "144p")
}

enum class AudioBitrate(val kbps: Int) {
    K320(320), K192(192), K128(128), K64(64)
}

data class ConvertRequest(
    val item: LocalMediaItem,
    val format: OutputFormat,
    val quality: VideoQuality,
    val audioBitrate: AudioBitrate,
    val startMs: Long,
    val endMs: Long
)

object MediaConverter {

    private fun seconds(ms: Long) = String.format(java.util.Locale.US, "%.3f", ms / 1000.0)

    private fun sourceExtension(context: Context, item: LocalMediaItem): String {
        val fromName = MediaFiles.displayName(context, Uri.parse(item.uri))
            ?.substringAfterLast('.', "")?.lowercase()
        if (!fromName.isNullOrBlank() && fromName.length <= 5) return fromName
        return when (item.mimeType) {
            "video/mp4" -> "mp4"
            "video/3gpp" -> "3gp"
            "video/x-matroska" -> "mkv"
            "video/webm" -> "webm"
            "audio/mpeg" -> "mp3"
            "audio/mp4", "audio/aac" -> "m4a"
            "audio/flac" -> "flac"
            "audio/ogg" -> "ogg"
            "audio/x-wav", "audio/wav" -> "wav"
            else -> if (item.mediaType == MediaType.VIDEO) "mp4" else "m4a"
        }
    }

    private fun videoArgs(req: ConvertRequest, encoder: String): List<String> {
        val args = mutableListOf<String>()
        val maxHeight = when {
            req.format == OutputFormat.THREE_GP -> minOf(req.quality.height ?: 288, 288)
            else -> req.quality.height
        }
        if (maxHeight != null) {
            // Only scale down, keep aspect ratio, keep sizes even (required by encoders).
            args += listOf("-vf", "scale=-2:'trunc(min($maxHeight,ih)/2)*2'")
        }
        if (encoder == "libx264") {
            args += listOf("-c:v", "libx264", "-preset", "veryfast", "-crf", "23", "-pix_fmt", "yuv420p")
            if (req.format == OutputFormat.THREE_GP) args += listOf("-profile:v", "baseline")
        } else {
            args += listOf("-c:v", "mpeg4", "-q:v", "5")
        }
        if (req.format == OutputFormat.THREE_GP) {
            args += listOf("-c:a", "aac", "-ac", "1", "-ar", "22050", "-b:a", "64k", "-f", "3gp")
        } else {
            args += listOf("-c:a", "aac", "-b:a", "${req.audioBitrate.kbps}k", "-movflags", "+faststart")
        }
        return args
    }

    /**
     * Converts and/or cuts the item. Returns the published file's Uri.
     * Throws with a readable message on failure.
     */
    suspend fun convert(
        context: Context,
        req: ConvertRequest,
        onProgress: (Float) -> Unit
    ): Uri {
        MediaEngine.ensureInit(context)
        val srcExt = sourceExtension(context, req.item)
        val input = MediaFiles.copyToCache(context, Uri.parse(req.item.uri), "input.$srcExt")
        try {
            val outExt = if (req.format == OutputFormat.ORIGINAL) srcExt else req.format.extension
            val output = File(input.parentFile, "output_${System.currentTimeMillis()}.$outExt")
            val start = req.startMs.coerceAtLeast(0)
            val end = if (req.endMs <= 0) req.item.durationMs else req.endMs
            val length = (end - start).coerceAtLeast(1000)
            val isFullLength = start == 0L && end >= req.item.durationMs - 300

            fun buildArgs(encoder: String): List<String> {
                val args = mutableListOf<String>()
                if (start > 0) args += listOf("-ss", seconds(start))
                args += listOf("-i", input.absolutePath)
                if (!isFullLength) args += listOf("-t", seconds(length))
                when (req.format) {
                    OutputFormat.ORIGINAL ->
                        args += listOf("-map", "0", "-c", "copy", "-avoid_negative_ts", "make_zero")
                    OutputFormat.MP3 ->
                        args += listOf("-vn", "-c:a", "libmp3lame", "-b:a", "${req.audioBitrate.kbps}k")
                    OutputFormat.M4A ->
                        args += listOf("-vn", "-c:a", "aac", "-b:a", "${req.audioBitrate.kbps}k")
                    OutputFormat.MP4, OutputFormat.THREE_GP -> args += videoArgs(req, encoder)
                }
                args += output.absolutePath
                return args
            }

            var result = MediaEngine.runFfmpeg(context, buildArgs("libx264"), length, onProgress)
            if (!result.success && req.format.isVideo && result.log.contains("libx264")) {
                // ffmpeg build without x264: fall back to the built-in MPEG-4 encoder.
                output.delete()
                result = MediaEngine.runFfmpeg(context, buildArgs("mpeg4"), length, onProgress)
            }
            if (!result.success || !output.exists() || output.length() == 0L) {
                output.delete()
                val reason = result.log.lineSequence()
                    .filter { it.isNotBlank() }
                    .toList()
                    .takeLast(3)
                    .joinToString("\n")
                throw IllegalStateException(reason.ifBlank { "ffmpeg failed (${result.exitCode})" })
            }
            onProgress(1f)
            val suffix = if (isFullLength) "" else "_cut"
            val name = MediaFiles.safeFileName(req.item.title) + suffix
            return MediaFiles.publish(context, output, name)
                ?: throw IllegalStateException("Could not save the file")
        } finally {
            input.delete()
        }
    }
}
