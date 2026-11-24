package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.media.AudioBitrate
import com.example.media.DownloadKind
import com.example.media.DownloadQuality
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.PulseCoral
import com.example.viewmodel.DownloadTask
import com.example.viewmodel.TaskStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadScreen(
    url: String,
    tasks: List<DownloadTask>,
    engineStatus: String?,
    hiddenCount: Int,
    onUrlChange: (String) -> Unit,
    onDownload: (DownloadKind, DownloadQuality, AudioBitrate) -> Unit,
    onCancel: (String) -> Unit,
    onClearFinished: () -> Unit,
    onUpdateEngine: () -> Unit,
    onRestoreHidden: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var kind by remember { mutableStateOf(DownloadKind.VIDEO) }
    var quality by remember { mutableStateOf(DownloadQuality.P720) }
    var bitrate by remember { mutableStateOf(AudioBitrate.K192) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                stringResource(R.string.download_header),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.download_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.download_url_hint)) },
                trailingIcon = {
                    IconButton(onClick = { clipboard.getText()?.text?.let(onUrlChange) }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(R.string.paste))
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = kind == DownloadKind.VIDEO,
                    onClick = { kind = DownloadKind.VIDEO },
                    label = { Text(stringResource(R.string.download_video)) }
                )
                FilterChip(
                    selected = kind == DownloadKind.AUDIO,
                    onClick = { kind = DownloadKind.AUDIO },
                    label = { Text(stringResource(R.string.download_audio)) }
                )
            }
            Spacer(Modifier.height(4.dp))
            if (kind == DownloadKind.VIDEO) {
                Text(stringResource(R.string.convert_quality), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DownloadQuality.entries.forEach { q ->
                        FilterChip(
                            selected = quality == q,
                            onClick = { quality = q },
                            label = { Text(if (q == DownloadQuality.BEST) stringResource(R.string.quality_best) else q.label) }
                        )
                    }
                }
            } else {
                Text(stringResource(R.string.convert_bitrate), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioBitrate.entries.forEach { b ->
                        FilterChip(
                            selected = bitrate == b,
                            onClick = { bitrate = b },
                            label = { Text("${b.kbps} kbps") }
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = { onDownload(kind, quality, bitrate) },
                enabled = url.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.download_button), fontWeight = FontWeight.SemiBold)
            }
            Text(
                stringResource(R.string.download_share_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (tasks.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.download_tasks),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (tasks.any { it.status != TaskStatus.RUNNING }) {
                        TextButton(onClick = onClearFinished) { Text(stringResource(R.string.download_clear)) }
                    }
                }
            }
            items(tasks, key = { it.id }) { task -> DownloadTaskCard(task, onCancel = { onCancel(task.id) }) }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onUpdateEngine, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.engine_update))
            }
            Text(
                engineStatus ?: stringResource(R.string.engine_update_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hiddenCount > 0) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRestoreHidden, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Restore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.restore_hidden, hiddenCount))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DownloadTaskCard(task: DownloadTask, onCancel: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    task.url,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                if (task.status == TaskStatus.RUNNING) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }
            }
            when (task.status) {
                TaskStatus.RUNNING -> {
                    if (task.progress in 0f..1f) {
                        LinearProgressIndicator(
                            progress = { task.progress },
                            color = PulseCoral,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(color = PulseCoral, modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        task.lastLine.ifBlank { stringResource(R.string.download_waiting) },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TaskStatus.DONE -> Text(stringResource(R.string.download_done), color = MaterialTheme.colorScheme.primary)
                TaskStatus.CANCELLED -> Text(stringResource(R.string.download_cancelled))
                TaskStatus.FAILED -> Text(
                    stringResource(R.string.convert_failed, task.error.orEmpty()),
                    color = ErrorRose,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
