package com.nruge.iceinfo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.model.TrainStop

@Composable
fun RecordJourneyDialog(
    status: TrainStatus,
    isRecording: Boolean,
    traewellingConnected: Boolean,
    onRecord: (recordGps: Boolean) -> Unit,
    onCheckIn: (exitStop: TrainStop) -> Unit,
    onDismiss: () -> Unit
) {
    val destination = status.targetStopEva
        ?.let { eva -> status.stops.find { it.evaNr == eva }?.name }
        ?: status.destination
    val origin = status.stops.firstOrNull { it.passed }?.name
        ?: status.stops.firstOrNull()?.name
        ?: stringResource(R.string.unknown)

    var recordGps by remember { mutableStateOf(false) }

    // Ausstiegs-Kandidaten: kommende, nicht ausgefallene Halte (Fallback: alle)
    val exitOptions = status.stops.filter { !it.passed && !it.isCancelled }
        .ifEmpty { status.stops.filter { !it.isCancelled } }
    // Auswahl über die stabile evaNr merken, damit der 5-s-Status-Refresh sie nicht
    // zurücksetzt; der Halt wird jeweils aus den aktuellen Optionen abgeleitet.
    var selectedExitEva by remember {
        mutableStateOf(status.targetStopEva ?: exitOptions.lastOrNull()?.evaNr)
    }
    val selectedExit = exitOptions.firstOrNull { it.evaNr == selectedExitEva }
        ?: exitOptions.lastOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Train,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${status.trainType} ${status.trainNumber}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$origin → $destination",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Teil 1: Fahrt aufzeichnen ────────────────────────────────
                SectionCard {
                    Text(
                        text = stringResource(R.string.record_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRecording) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.record_running),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.GpsFixed,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (recordGps) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.record_gps_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.record_gps_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = recordGps, onCheckedChange = { recordGps = it })
                        }
                        Button(
                            onClick = { onRecord(recordGps) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.record_confirm))
                        }
                    }
                }

                // ── Teil 2: Bei Träwelling einchecken (nur wenn verbunden) ────
                if (traewellingConnected) {
                    SectionCard {
                        Text(
                            text = stringResource(R.string.checkin_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = selectedExit?.name
                                        ?: stringResource(R.string.checkin_exit_label),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start,
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                exitOptions.forEach { stop ->
                                    DropdownMenuItem(
                                        text = { Text(stop.name) },
                                        onClick = { selectedExitEva = stop.evaNr; expanded = false }
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { selectedExit?.let(onCheckIn) },
                            enabled = selectedExit != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Train, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.checkin_button))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}
