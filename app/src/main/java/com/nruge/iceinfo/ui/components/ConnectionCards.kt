package com.nruge.iceinfo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.ConnectingTrain
import com.nruge.iceinfo.model.Departure
import com.nruge.iceinfo.ui.theme.onSuccessContainer
import com.nruge.iceinfo.ui.theme.onWarningContainer
import com.nruge.iceinfo.ui.theme.rainbowColor
import com.nruge.iceinfo.ui.theme.successContainer
import com.nruge.iceinfo.ui.theme.warningContainer
import com.nruge.iceinfo.util.formatRemainingTimeUntil
import java.time.LocalTime

@Composable
fun ConnectionCardContent(
    conn: ConnectingTrain,
    showRelative: Boolean = false,
    referenceTime: LocalTime = LocalTime.now()
) {
    val isTight = conn.reachable && conn.transferMinutes != null && conn.transferMinutes < 5

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                !conn.reachable -> MaterialTheme.colorScheme.errorContainer
                isTight -> warningContainer()
                else -> successContainer()
            },
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Train,
                    contentDescription = null,
                    tint = when {
                        !conn.reachable -> MaterialTheme.colorScheme.onErrorContainer
                        isTight -> onWarningContainer()
                        else -> onSuccessContainer()
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrainTypeBadge(conn.trainType)
                Text(
                    text = conn.trainNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = conn.destination,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            DepartureTimePair(
                scheduled = conn.departure,
                delayMinutes = conn.delayMinutes,
                showRelative = showRelative,
                referenceTime = referenceTime,
                cancelled = false
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (conn.track.isNotEmpty()) {
                TrackLabel(
                    text = stringResource(R.string.track_short, conn.track),
                    changed = conn.trackChanged
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = stringResource(R.string.track_short, conn.track),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (conn.transferMinutes != null) {
                Text(
                    text = stringResource(R.string.connection_transfer_minutes, conn.transferMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        !conn.reachable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        isTight -> onWarningContainer()
                        else -> onSuccessContainer()
                    }
                )
            }
        }
    }
}

@Composable
fun DepartureCardContent(
    dep: Departure,
    showRelative: Boolean = false,
    referenceTime: LocalTime = LocalTime.now(),
    transferMinutes: Int? = null
) {
    val isCancelled = dep.cancelled
    val isMissed = isCancelled || (transferMinutes != null && transferMinutes < 0)
    val isTight = !isMissed && transferMinutes != null && transferMinutes < 5

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                isMissed -> MaterialTheme.colorScheme.errorContainer
                isTight -> warningContainer()
                transferMinutes != null -> successContainer()
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Train,
                    contentDescription = null,
                    tint = when {
                        isMissed -> MaterialTheme.colorScheme.onErrorContainer
                        isTight -> onWarningContainer()
                        transferMinutes != null -> onSuccessContainer()
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val parts = dep.line.trim().split(" ", limit = 2)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (parts.size == 2) {
                    TrainTypeBadge(parts[0], muted = isCancelled)
                    Text(
                        text = parts[1],
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCancelled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = dep.line,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isCancelled) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = stringResource(R.string.stop_cancelled),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Text(
                text = dep.destination,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isCancelled) 0.5f else 1f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            DepartureTimePair(
                scheduled = dep.scheduledTime,
                delayMinutes = dep.delayMinutes,
                cancelled = isCancelled,
                showRelative = showRelative && !isCancelled,
                referenceTime = referenceTime
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (dep.platform.isNotEmpty()) {
                TrackLabel(
                    text = stringResource(R.string.track_short, dep.platform),
                    changed = dep.platformChanged
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = stringResource(R.string.track_short, dep.platform),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (transferMinutes != null && !isCancelled && transferMinutes >= 0) {
                Text(
                    text = stringResource(R.string.connection_transfer_minutes, transferMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isMissed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        isTight -> onWarningContainer()
                        else -> onSuccessContainer()
                    }
                )
            }
        }
    }
}

@Composable
private fun DepartureTimePair(
    scheduled: String,
    delayMinutes: Int,
    cancelled: Boolean = false,
    showRelative: Boolean = false,
    referenceTime: LocalTime = LocalTime.now()
) {
    val actual = addMinutesToTime(scheduled, delayMinutes)
    val isDelayed = delayMinutes != 0 && !cancelled
    val isEarly = delayMinutes < 0 && !cancelled
    val relativeText = if (showRelative) {
        formatRemainingTimeUntil(scheduled, delayMinutes, referenceTime).takeIf { it != "--" }?.let { "in $it" }
    } else null

    val depRelAlpha by animateFloatAsState(
        targetValue = if (relativeText != null) 1f else 0f,
        animationSpec = tween(350),
        label = "dep_rel_alpha"
    )

    Box(contentAlignment = Alignment.CenterStart) {
        Row(
            modifier = Modifier.alpha(1f - depRelAlpha),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = scheduled,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    cancelled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    isDelayed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else      -> MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if ((isDelayed || cancelled) && scheduled.isNotEmpty()) TextDecoration.LineThrough else TextDecoration.None
            )
            if (isEarly) {
                Text(
                    text = actual,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = rainbowColor()
                )
            } else {
                Text(
                    text = actual,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        cancelled                      -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        isDelayed && delayMinutes >= 5 -> MaterialTheme.colorScheme.error
                        else                           -> onSuccessContainer()
                    },
                    textDecoration = if (cancelled) TextDecoration.LineThrough else TextDecoration.None
                )
            }
        }
        Text(
            text = relativeText ?: "",
            modifier = Modifier.alpha(depRelAlpha),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = when {
                cancelled                      -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                isDelayed && delayMinutes >= 5 -> MaterialTheme.colorScheme.error
                isEarly                        -> rainbowColor()
                else                           -> onSuccessContainer()
            }
        )
    }
}

@Composable
private fun TrainTypeBadge(type: String, muted: Boolean = false) {
    Surface(
        color = if (muted) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = type,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (muted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun addMinutesToTime(time: String, minutes: Int): String {
    if (minutes == 0) return time
    val parts = time.split(":")
    if (parts.size != 2) return time
    val h = parts[0].toIntOrNull() ?: return time
    val m = parts[1].toIntOrNull() ?: return time
    val total = h * 60 + m + minutes
    return "%02d:%02d".format((total / 60) % 24, total % 60)
}
