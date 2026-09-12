package com.nruge.iceinfo.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.nruge.iceinfo.MenuRepository
import com.nruge.iceinfo.R
import com.nruge.iceinfo.TrainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class ApiStatusItem(val label: String, val ok: Boolean, val detail: String, val body: String? = null)

private suspend fun checkExternalUrl(label: String, url: String): ApiStatusItem {
    return withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout    = 5_000
            conn.requestMethod  = "GET"
            val code = conn.responseCode
            val body = runCatching { conn.inputStream.bufferedReader().readText() }.getOrNull()
            conn.disconnect()
            val ok = code in 200..499
            ApiStatusItem(label, ok, if (ok) "OK" else "HTTP $code", body)
        } catch (e: Exception) {
            ApiStatusItem(label, false, (e.message ?: e.javaClass.simpleName).take(50))
        }
    }
}

@Composable
fun DebugDialog(
    trainConnected: Boolean,
    simulateTrainWifi: Boolean = false,
    onToggleSimulateTrainWifi: ((Boolean) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var iceStatuses by remember { mutableStateOf<List<ApiStatusItem>>(emptyList()) }
    var extStatuses by remember { mutableStateOf<List<ApiStatusItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val appVersion = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?" }
        catch (_: PackageManager.NameNotFoundException) { "?" }
    }
    val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val device = "${Build.MANUFACTURER} ${Build.MODEL}"

    LaunchedEffect(Unit) {
        // Externe Checks parallel starten (unabhängig vom ICE-Portal)
        val extAsync = async {
            listOf(
                "Open-Meteo"       to "https://api.open-meteo.com/v1/forecast?latitude=52&longitude=13&hourly=temperature_2m&forecast_days=1",
                "bahn.de Abfahrten" to "https://www.bahn.de/web/api/reiseloesung/abfahrten?ortExtId=8000105",
                "transport.rest"   to "https://v6.db.transport.rest/stations/8000105",
                "DB StaDa"         to "https://apis.deutschebahn.com/db-api-marketplace/apis/station-data/v2/stations?limit=1",
                "DB FaSta"         to "https://apis.deutschebahn.com/db-api-marketplace/apis/fasta/v2/facilities?limit=1",
                "Wagenreihung"     to "https://www.bahn.de/web/api/reisebegleitung/wagenreihung/vehicle-sequence",
                "OSM Overpass"     to "https://overpass-api.de/api/interpreter",
            ).map { (label, url) -> checkExternalUrl(label, url) }
        }

        if (trainConnected) {
            iceStatuses = (TrainRepository.checkEndpoints() + MenuRepository.checkEndpoints())
                .map { ApiStatusItem(it.label, it.ok, it.detail, it.body) }
        } else {
            iceStatuses = (TrainRepository.endpointLabels() + MenuRepository.endpointLabels())
                .map { ApiStatusItem(it, false, "kein ICE-WLAN") }
        }

        extStatuses = extAsync.await()
        isLoading   = false
    }

    val timestamp = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault()).format(Instant.now())
    }

    fun buildHeader(sb: StringBuilder) {
        sb.appendLine("=== ICE Info Debug Report ===")
        sb.appendLine("Zeitstempel: $timestamp")
        sb.appendLine("Betriebssystem: $osVersion")
        sb.appendLine("Gerät: $device")
        sb.appendLine("App-Version: $appVersion")
        sb.appendLine()
        sb.appendLine("--- ICE Portal APIs ---")
        iceStatuses.forEach { sb.appendLine("${it.label.padEnd(22)} ${if (it.ok) it.detail else "FEHLER: ${it.detail}"}") }
        sb.appendLine()
        sb.appendLine("--- Externe APIs ---")
        extStatuses.forEach { sb.appendLine("${it.label.padEnd(22)} ${if (it.ok) it.detail else "FEHLER: ${it.detail}"}") }
    }

    fun StringBuilder.appendEndpoints(items: List<ApiStatusItem>, maxLines: Int) {
        items.forEach { item ->
            appendLine("--- ${item.label} ---")
            when {
                !item.ok -> appendLine("FEHLER: ${item.detail}")
                item.body == null -> appendLine("(kein Body)")
                else -> {
                    val lines = item.body.lines()
                    appendLine(lines.take(maxLines).joinToString("\n"))
                    if (lines.size > maxLines) appendLine("… (${lines.size - maxLines} weitere Zeilen)")
                }
            }
            appendLine()
        }
    }

    // Clipboard: Status-Tabelle + erste 6 Zeilen je Endpoint
    val clipboardText = remember(iceStatuses, extStatuses) {
        buildString {
            buildHeader(this)
            appendLine()
            appendLine("--- ICE Portal APIs ---")
            appendEndpoints(iceStatuses, 6)
            appendLine("--- Externe APIs ---")
            appendEndpoints(extStatuses, 6)
        }
    }

    // Datei: erste 50 Zeilen je Endpoint
    val fileText = remember(iceStatuses, extStatuses) {
        buildString {
            buildHeader(this)
            appendLine()
            appendLine("--- ICE Portal APIs ---")
            appendEndpoints(iceStatuses, 50)
            appendLine("--- Externe APIs ---")
            appendEndpoints(extStatuses, 50)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.BugReport, contentDescription = null) },
        title = { Text(stringResource(R.string.debug_title), fontWeight = FontWeight.Bold) },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.debug_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DebugCard(title = stringResource(R.string.debug_section_device)) {
                        DebugRow(stringResource(R.string.debug_os), osVersion)
                        DebugRow(stringResource(R.string.debug_device), device)
                        DebugRow(stringResource(R.string.debug_app_version), appVersion)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ApiStatusRow(
                            ApiStatusItem(
                                label = "ICE Portal",
                                ok = trainConnected,
                                detail = if (trainConnected) "verbunden" else "nicht verbunden"
                            )
                        )
                        if (com.nruge.iceinfo.BuildConfig.DEBUG && onToggleSimulateTrainWifi != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.debug_simulate_wifi),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = simulateTrainWifi,
                                    onCheckedChange = onToggleSimulateTrainWifi
                                )
                            }
                        }
                    }

                    DebugCard(title = "ICE Portal") {
                        iceStatuses.forEachIndexed { i, item ->
                            if (i > 0) HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            ApiStatusRow(item)
                        }
                    }

                    DebugCard(title = "Externe APIs") {
                        extStatuses.forEachIndexed { i, item ->
                            if (i > 0) HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            ApiStatusRow(item)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("ICE Debug", clipboardText))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.debug_copy))
                        }
                        OutlinedButton(
                            onClick = { shareDebugText(context, fileText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.debug_share))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.info_close))
            }
        }
    )
}

@Composable
private fun ApiStatusRow(item: ApiStatusItem) {
    val dotColor = if (item.ok) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        VerticalDivider(
            modifier = Modifier
                .height(14.dp)
                .padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Icon(
            imageVector = Icons.Filled.Circle,
            contentDescription = null,
            tint = dotColor,
            modifier = Modifier.size(8.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = item.detail,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (item.ok) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun DebugCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun shareDebugText(context: Context, text: String) {
    val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        .withZone(ZoneId.systemDefault()).format(Instant.now())
    val dir = File(context.cacheDir, "debug").also { it.mkdirs() }
    val file = File(dir, "ice_debug_$timestamp.txt")
    file.writeText(text)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.debug_share_chooser)))
}
