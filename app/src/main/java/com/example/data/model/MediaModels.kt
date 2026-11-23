package com.example.data.model

import java.util.Locale

enum class MediaType {
    AUDIO,
    VIDEO
}

data class LocalMediaItem(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dateAddedSec: Long,
    val sizeBytes: Long,
    val folderPath: String,
    val folderName: String,
    val mediaType: MediaType,
    val albumArtUri: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = ""
) {
    val resolutionLabel: String
        get() = when {
            width <= 0 || height <= 0 -> ""
            maxOf(width, height) >= 3840 -> "4K"
            maxOf(width, height) >= 2560 -> "2K"
            minOf(width, height) >= 1080 -> "1080p"
            minOf(width, height) >= 720 -> "720p"
            minOf(width, height) >= 480 -> "480p"
            else -> "${width}×${height}"
        }
}

data class MediaFolder(
    val path: String,
    val name: String,
    val audioCount: Int,
    val videoCount: Int,
    val totalCount: Int,
    val previewUri: String?,
    val previewIsVideo: Boolean
)

enum class AudioSortOption {
    NAME,
    DATE_ADDED,
    DURATION
}

enum class RepeatModeOption {
    OFF,
    ALL,
    ONE
}

enum class SleepTimerOption(val minutes: Int?) {
    OFF(null),
    MIN_15(15),
    MIN_30(30),
    MIN_60(60)
}

object MediaFormatters {
    fun formatDuration(durationMs: Long): String {
        val safeMs = durationMs.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0L) return "0 B"
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$sizeBytes B"
        }
    }

    fun formatSpeed(speed: Float): String {
        return if (speed % 1.0f == 0f) {
            String.format(Locale.US, "%.1fx", speed)
        } else {
            String.format(Locale.US, "%.2fx", speed).trimEnd('0')
        }
    }
}
