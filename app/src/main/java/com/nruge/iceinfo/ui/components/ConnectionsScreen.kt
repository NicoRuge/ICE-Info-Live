package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.ConnectingTrain
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.sampleConnections

@Composable
fun ConnectionsScreen(
    status: TrainStatus,
    connections: List<ConnectingTrain>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val displayConnections =
            if (status.isConnected && connections.isNotEmpty()) connections
            else sampleConnections

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.connections_title, status.nextStop),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!status.isConnected) {
                        Text(
                            text = stringResource(R.string.pois_demo),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                displayConnections.forEachIndexed { index, conn ->
                    ConnectionRow(conn)
                    if (index < displayConnections.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionRow(conn: ConnectingTrain) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        overlineContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${conn.trainType} ${conn.trainNumber}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            text = if (conn.reachable) stringResource(R.string.connection_reachable)
                                   else stringResource(R.string.connection_missed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = if (conn.reachable)
                            MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer,
                        disabledLabelColor = if (conn.reachable)
                            MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                    ),
                    border = null
                )
            }
        },
        headlineContent = {
            Text(
                text = "→ ${conn.destination}",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = conn.departure,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (conn.track.isNotEmpty()) {
                    Text(
                        text = "Gl. ${conn.track}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (conn.delayMinutes > 0) {
                    DelayBadge(delayMinutes = conn.delayMinutes, size = DelayBadgeSize.SMALL)
                }
            }
        }
    )
}