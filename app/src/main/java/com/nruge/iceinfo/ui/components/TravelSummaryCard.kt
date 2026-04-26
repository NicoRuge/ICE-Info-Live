package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.ui.theme.Green40
import com.nruge.iceinfo.ui.theme.Green90
import com.nruge.iceinfo.util.formatRemainingTime

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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${status.stops.firstOrNull()?.name ?: "—"} ➜ $displayDestination",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.travel_remaining_km, remainingDistanceToTarget / 1000),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (targetStop != null) 
                        "Noch ${stopsToTarget.size} Halte" 
                        else stringResource(R.string.travel_stops_progress, passedStops, totalStopsInJourney),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.travel_arrival, displayEta),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(
                            R.string.travel_remaining_time,
                            formatRemainingTime(remainingDistanceToTarget, status.speed)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (displayDelay > 0) {
                    DelayBadge(delayMinutes = displayDelay)
                } else {
                    Surface(color = Green90, shape = MaterialTheme.shapes.small) {
                        Text(
                            text = stringResource(R.string.travel_on_time),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Green40,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
