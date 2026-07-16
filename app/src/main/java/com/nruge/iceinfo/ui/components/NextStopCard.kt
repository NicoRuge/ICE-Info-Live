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
import com.nruge.iceinfo.ui.theme.onSuccessContainer
import java.time.LocalTime

@Composable
fun NextStopCard(
    status: TrainStatus,
    showRelative: Boolean = false,
    referenceTime: LocalTime = LocalTime.now()
) {
    val nextStop = status.stops.firstOrNull { it.isNext }
    val scheduledArrival = nextStop?.scheduledArrival ?: ""
    val delay = status.delayMinutes
    val isDelayed = delay > 0
    val isEarly = delay < 0
    val delayColor = when {
        isEarly -> MaterialTheme.colorScheme.tertiary
        delay >= 5 -> MaterialTheme.colorScheme.error
        isDelayed -> onSuccessContainer()
        else -> MaterialTheme.colorScheme.tertiary
    }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.home_next_stop_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.nextStop,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                if (status.eta.isNotEmpty()) {
                    ArrivalTimeToggle(
                        scheduled = scheduledArrival,
                        actual = status.eta,
                        delay = delay,
                        showRelative = showRelative,
                        referenceTime = referenceTime
                    )
                }
            }
            if (status.track.isNotEmpty() || delay != 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (status.track.isNotEmpty()) {
                        TrackLabel(
                            text = stringResource(R.string.track_full, status.track),
                            changed = status.trackChanged,
                            style = MaterialTheme.typography.bodySmall
                        ) {
                            Text(
                                text = stringResource(R.string.track_full, status.track),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (delay != 0) {
                        Text(
                            text = if (isEarly) "$delay min" else "+$delay min",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = delayColor
                        )
                    }
                }
            }
        }
    }
}
