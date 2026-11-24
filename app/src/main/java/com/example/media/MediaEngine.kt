package com.example.media

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wraps the native yt-dlp (python) + ffmpeg binaries shipped by youtubedl-android.
 * The same ffmpeg binary is used directly for converting and cutting local files.
 */
object MediaEngine {

    @Volatile
    private var initialized = false

    suspend fun ensureInit(context: Context) = withContext(Dispatchers.IO) {
        synchronized(this@MediaEngine) {
            if (!initialized) {
                val app = context.applicationContext
                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)
                initialized = true
            }
        }
    }

    private fun ffmpegBinary(context: Context): File =
        File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so")

    private fun libraryPath(context: Context): String {
        val packages = File(File(context.noBackupFilesDir, "youtubedl-android"), "packages")
        return listOf("python", "ffmpeg", "aria2c")
            .joinToString(":") { File(packages, it).absolutePath + "/usr/lib" }
    }

    class FfmpegResult(val exitCode: Int, val log: String) {
        val success get() = exitCode == 0
    }

    /**
     * Runs ffmpeg with [args]. [totalDurationMs] is used to turn ffmpeg's "time=" output
     * into a 0..1 progress value. Cancelling the coroutine kills the process.
     */
    suspend fun runFfmpeg(
        context: Context,
        args: List<String>,
        totalDurationMs: Long,
        onProgress: (Float) -> Unit
    ): FfmpegResult = withContext(Dispatchers.IO) {
        ensureInit(context)
        val command = listOf(ffmpegBinary(context).absolutePath, "-hide_banner", "-nostdin", "-y") + args
        val builder = ProcessBuilder(command).redirectErrorStream(true)
        builder.environment().apply {
            this["LD_LIBRARY_PATH"] = libraryPath(context)
            this["TMPDIR"] = context.cacheDir.absolutePath
            this["HOME"] = context.filesDir.absolutePath
        }
        val process = builder.start()
        val log = StringBuilder()
        val timeRegex = Regex("""time=(\d+):(\d+):(\d+(?:\.\d+)?)""")
        coroutineScope {
            // Kills ffmpeg as soon as the caller cancels (this also unblocks readLine()).
            val killer = launch {
                try {
                    awaitCancellation()
                } finally {
                    process.destroy()
                }
            }
            try {
                process.inputStream.bufferedReader().use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (log.length > 20_000) log.delete(0, 5_000)
                        log.appendLine(line)
                        val match = timeRegex.find(line)
                        if (match != null && totalDurationMs > 0) {
                            val (h, m, sec) = match.destructured
                            val ms = ((h.toLong() * 3600 + m.toLong() * 60) * 1000) +
                                (sec.toDouble() * 1000).toLong()
                            onProgress((ms.toFloat() / totalDurationMs).coerceIn(0f, 1f))
                        }
                    }
                }
            } catch (e: java.io.IOException) {
                // Stream closed because the process was killed
            }
            ensureActive()
            val code = process.waitFor()
            killer.cancel()
            FfmpegResult(code, log.toString())
        }
    }
}
