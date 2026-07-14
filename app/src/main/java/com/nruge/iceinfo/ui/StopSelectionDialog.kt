package com.nruge.iceinfo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStop
import com.nruge.iceinfo.ui.components.TrackLabel

@Composable
fun StopSelectionDialog(
    stops: List<TrainStop>,
    onStopSelected: (TrainStop) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stop_selection_title)) },
        text = {
            val upcomingStops = stops.filter { !it.passed }
            if (upcomingStops.isEmpty()) {
                Text(stringResource(R.string.stop_selection_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(upcomingStops) { stop ->
                        ListItem(
                            headlineContent = { Text(stop.name, fontWeight = FontWeight.Bold) },
                            supportingContent = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.stop_selection_arrival, stop.scheduledArrival))
                                    if (stop.track.isNotEmpty()) {
                                        TrackLabel(
                                            text = stringResource(R.string.track_full, stop.track),
                                            changed = stop.trackChanged,
                                            style = MaterialTheme.typography.bodyMedium
                                        ) {
                                            Text(stringResource(R.string.track_full, stop.track))
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onStopSelected(stop) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
