package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.media.AudioBitrate
import com.example.media.ConvertRequest
import com.example.media.DownloadKind
import com.example.media.DownloadQuality
import com.example.media.MediaConverter
import com.example.media.MediaDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class TaskStatus { RUNNING, DONE, FAILED, CANCELLED }

data class DownloadTask(
    val id: String,
    val url: String,
    val kind: DownloadKind,
    val progress: Float = -1f,
    val lastLine: String = "",
    val status: TaskStatus = TaskStatus.RUNNING,
    val error: String? = null
)

data class ConvertJobState(
    val title: String,
    val progress: Float = 0f,
    val status: TaskStatus = TaskStatus.RUNNING,
    val error: String? = null
)

/** Downloads and conversions. Calls [onLibraryChanged] when a new file was saved. */
class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    var onLibraryChanged: () -> Unit = {}

    private val _downloads = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloads: StateFlow<List<DownloadTask>> = _downloads.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _engineStatus = MutableStateFlow<String?>(null)
    val engineStatus: StateFlow<String?> = _engineStatus.asStateFlow()

    private val _convertJob = MutableStateFlow<ConvertJobState?>(null)
    val convertJob: StateFlow<ConvertJobState?> = _convertJob.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()
    private var convertCoroutine: Job? = null

    fun setUrl(url: String) {
        _urlInput.value = url
    }

    fun startDownload(kind: DownloadKind, quality: DownloadQuality, bitrate: AudioBitrate) {
        val url = extractUrl(_urlInput.value) ?: return
        val id = UUID.randomUUID().toString()
        _downloads.update { listOf(DownloadTask(id = id, url = url, kind = kind)) + it }
        _urlInput.value = ""
        jobs[id] = viewModelScope.launch {
            try {
                MediaDownloader.download(
                    context = getApplication(),
                    url = url,
                    kind = kind,
                    quality = quality,
                    audioBitrate = bitrate,
                    processId = id
                ) { progress, line ->
                    updateTask(id) { it.copy(progress = progress, lastLine = line.take(160)) }
                }
                updateTask(id) { it.copy(status = TaskStatus.DONE, progress = 1f) }
                onLibraryChanged()
            } catch (e: CancellationException) {
                updateTask(id) { it.copy(status = TaskStatus.CANCELLED) }
            } catch (e: Throwable) {
                val cancelled = _downloads.value.firstOrNull { it.id == id }?.status == TaskStatus.CANCELLED
                if (!cancelled) {
                    updateTask(id) {
                        it.copy(status = TaskStatus.FAILED, error = e.message?.lines()?.lastOrNull { l -> l.isNotBlank() } ?: e.toString())
                    }
                }
            } finally {
                jobs.remove(id)
            }
        }
    }

    fun cancelDownload(id: String) {
        updateTask(id) { it.copy(status = TaskStatus.CANCELLED) }
        MediaDownloader.cancel(id)
        jobs[id]?.cancel()
    }

    fun clearFinished() {
        _downloads.update { list -> list.filter { it.status == TaskStatus.RUNNING } }
    }

    fun updateEngine() {
        _engineStatus.value = "…"
        viewModelScope.launch {
            _engineStatus.value = try {
                MediaDownloader.updateEngine(getApplication())
            } catch (e: Throwable) {
                e.message ?: e.toString()
            }
        }
    }

    fun startConvert(request: ConvertRequest) {
        if (convertCoroutine?.isActive == true) return
        _convertJob.value = ConvertJobState(title = request.item.title)
        convertCoroutine = viewModelScope.launch {
            try {
                MediaConverter.convert(getApplication(), request) { p ->
                    _convertJob.update { it?.copy(progress = p) }
                }
                _convertJob.update { it?.copy(status = TaskStatus.DONE, progress = 1f) }
                onLibraryChanged()
            } catch (e: CancellationException) {
                _convertJob.update { it?.copy(status = TaskStatus.CANCELLED) }
            } catch (e: Throwable) {
                _convertJob.update { it?.copy(status = TaskStatus.FAILED, error = e.message ?: e.toString()) }
            }
        }
    }

    fun cancelConvert() {
        convertCoroutine?.cancel()
        _convertJob.update { it?.copy(status = TaskStatus.CANCELLED) }
    }

    fun dismissConvertJob() {
        if (convertCoroutine?.isActive != true) _convertJob.value = null
    }

    private fun updateTask(id: String, change: (DownloadTask) -> DownloadTask) {
        _downloads.update { list -> list.map { if (it.id == id) change(it) else it } }
    }

    companion object {
        private val urlRegex = Regex("""https?://\S+""")

        /** Shared text often contains a title before the link: keep only the link. */
        fun extractUrl(text: String): String? =
            urlRegex.find(text)?.value?.trimEnd('.', ',', ')', ']')
                ?: text.trim().takeIf { it.contains('.') && !it.contains(' ') }?.let { "https://$it" }
    }
}
