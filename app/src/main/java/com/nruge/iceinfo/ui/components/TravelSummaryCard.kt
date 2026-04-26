package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.util.formatRemainingTime

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TravelSummaryCard(status: TrainStatus) {
    val targetStop = status.stops.find { it.evaNr == status.targetStopEva }
    val displayDestination = targetStop?.name ?: status.destination
    val displayEta = targetStop?.actualArrival ?: status.destinationEta
    val displayDelay = targetStop?.delayMinutes ?: status.destinationDelay

    // Calculate progress towards selection
    val stopsToTarget = if (targetStop != null) {
        status.stops.filter { !it.passed && it.distanceFromStart <= targetStop.distanceFromStart }
    } else {
        status.stops.filter { !it.passed }
    }
    
    val totalStopsInJourney = status.stops.size
    val passedStops = status.stops.count { it.passed }
    
    // Distance calculation
    val targetDistance = targetStop?.distanceFromStart ?: status.stops.lastOrNull()?.distanceFromStart ?: 0
    val currentPosition = status.actualPosition
    val remainingDistanceToTarget = (targetDistance - currentPosition).coerceAtLeast(0)
    
    val totalDistanceForProgress = targetDistance.toFloat()
    val progressPercent = if (totalDistanceForProgress > 0) {
        (currentPosition.toFloat() / totalDistanceForProgress).coerceIn(0f, 1f)
    } else 0f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${status.stops.firstOrNull()?.name ?: "—"} ➜ $displayDestination",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.travel_remaining_km, remainingDistanceToTarget / 1000),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (targetStop != null) 
                        "Noch ${stopsToTarget.size} Halte" 
                        else stringResource(R.string.travel_stops_progress, passedStops, totalStopsInJourney),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            if (status.speed > 0) {
                LinearWavyProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    waveSpeed = (status.speed / 15f).coerceAtMost(20f).dp
                )
            } else {
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.travel_arrival, displayEta),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(
                            R.string.travel_remaining_time,
                            formatRemainingTime(remainingDistanceToTarget, status.speed)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                if (displayDelay > 0) {
                    DelayBadge(delayMinutes = displayDelay)
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), 
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(R.string.travel_on_time),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
