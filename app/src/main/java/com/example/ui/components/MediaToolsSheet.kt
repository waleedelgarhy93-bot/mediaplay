package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LocalMediaItem
import com.example.data.model.MediaFormatters
import com.example.media.AudioBitrate
import com.example.media.ConvertRequest
import com.example.media.OutputFormat
import com.example.media.VideoQuality
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.PulseCoral
import com.example.viewmodel.ConvertJobState
import com.example.viewmodel.TaskStatus

/** Opens the "Cut / Convert / Delete" sheet for an item. Provided by PulsePlayApp. */
val LocalMediaToolsLauncher = staticCompositionLocalOf<(LocalMediaItem) -> Unit> { {} }

/** Menu entry added to every audio/video item's "more" menu. */
@Composable
fun MediaToolsMenuItem(item: LocalMediaItem, onDismissMenu: () -> Unit) {
    val open = LocalMediaToolsLauncher.current
    DropdownMenuItem(
        text = { Text(stringResource(R.string.media_tools)) },
        leadingIcon = { Icon(Icons.Rounded.Build, contentDescription = null) },
        onClick = {
            onDismissMenu()
            open(item)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaToolsSheet(
    item: LocalMediaItem,
    onConvert: () -> Unit,
    onHide: () -> Unit,
    onDeleteFromPhone: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            ToolRow(Icons.Rounded.ContentCut, stringResource(R.string.tools_convert), stringResource(R.string.tools_convert_desc)) {
                onConvert()
            }
            ToolRow(Icons.Rounded.VisibilityOff, stringResource(R.string.tools_hide), stringResource(R.string.tools_hide_desc)) {
                onHide()
            }
            ToolRow(
                Icons.Rounded.DeleteForever,
                stringResource(R.string.tools_delete),
                stringResource(R.string.tools_delete_desc),
                tint = ErrorRose
            ) {
                confirmDelete = true
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_msg, item.title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteFromPhone()
                }) { Text(stringResource(R.string.delete), color = ErrorRose) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = tint, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConvertDialog(
    item: LocalMediaItem,
    job: ConvertJobState?,
    onStart: (ConvertRequest) -> Unit,
    onCancelJob: () -> Unit,
    onDismiss: () -> Unit
) {
    val duration = item.durationMs.coerceAtLeast(1000L).toFloat()
    var range by remember { mutableStateOf(0f..duration) }
    val formats = remember(item.mediaType) { OutputFormat.choicesFor(item.mediaType) }
    var format by remember { mutableStateOf(formats.first()) }
    var quality by remember { mutableStateOf(VideoQuality.ORIGINAL) }
    var bitrate by remember { mutableStateOf(AudioBitrate.K192) }
    val running = job?.status == TaskStatus.RUNNING

    AlertDialog(
        onDismissRequest = { if (!running) onDismiss() },
        title = { Text(stringResource(R.string.convert_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))

                Text(stringResource(R.string.convert_range), style = MaterialTheme.typography.labelLarge)
                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    valueRange = 0f..duration,
                    enabled = !running
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.convert_start) + ": " + MediaFormatters.formatDuration(range.start.toLong()),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        stringResource(R.string.convert_end) + ": " + MediaFormatters.formatDuration(range.endInclusive.toLong()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(12.dp))

                Text(stringResource(R.string.convert_format), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    formats.forEach { f ->
                        FilterChip(
                            selected = format == f,
                            onClick = { format = f },
                            enabled = !running,
                            label = { Text(if (f == OutputFormat.ORIGINAL) stringResource(R.string.format_original) else f.label) }
                        )
                    }
                }

                if (format.isVideo) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.convert_quality), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VideoQuality.entries.forEach { q ->
                            FilterChip(
                                selected = quality == q,
                                onClick = { quality = q },
                                enabled = !running,
                                label = { Text(if (q == VideoQuality.ORIGINAL) stringResource(R.string.quality_original) else q.label) }
                            )
                        }
                    }
                }
                if (format != OutputFormat.ORIGINAL && format != OutputFormat.THREE_GP) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.convert_bitrate), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioBitrate.entries.forEach { b ->
                            FilterChip(
                                selected = bitrate == b,
                                onClick = { bitrate = b },
                                enabled = !running,
                                label = { Text("${b.kbps} kbps") }
                            )
                        }
                    }
                }

                if (job != null) {
                    Spacer(Modifier.height(16.dp))
                    when (job.status) {
                        TaskStatus.RUNNING -> {
                            LinearProgressIndicator(
                                progress = { job.progress },
                                color = PulseCoral,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                stringResource(R.string.convert_running, (job.progress * 100).toInt()),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TaskStatus.DONE -> Text(stringResource(R.string.convert_done), color = MaterialTheme.colorScheme.primary)
                        TaskStatus.FAILED -> Text(stringResource(R.string.convert_failed, job.error.orEmpty()), color = ErrorRose)
                        TaskStatus.CANCELLED -> Text(stringResource(R.string.download_cancelled))
                    }
                }
            }
        },
        confirmButton = {
            if (running) {
                OutlinedButton(onClick = onCancelJob) { Text(stringResource(R.string.cancel)) }
            } else {
                Button(onClick = {
                    onStart(
                        ConvertRequest(
                            item = item,
                            format = format,
                            quality = quality,
                            audioBitrate = bitrate,
                            startMs = range.start.toLong(),
                            endMs = range.endInclusive.toLong()
                        )
                    )
                }) { Text(stringResource(R.string.convert_start_button)) }
            }
        },
        dismissButton = {
            if (!running) TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
