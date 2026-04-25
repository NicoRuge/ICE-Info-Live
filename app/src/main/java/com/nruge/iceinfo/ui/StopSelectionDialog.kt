package com.nruge.iceinfo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.model.TrainStop

@Composable
fun StopSelectionDialog(
    stops: List<TrainStop>,
    onStopSelected: (TrainStop) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ziel wählen") },
        text = {
            val upcomingStops = stops.filter { !it.passed }
            if (upcomingStops.isEmpty()) {
                Text("Keine kommenden Halte verfügbar.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(upcomingStops) { stop ->
                        ListItem(
                            headlineContent = { Text(stop.name, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("Ankunft: ${stop.scheduledArrival} · Gleis ${stop.track}") },
                            modifier = Modifier.clickable { onStopSelected(stop) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
