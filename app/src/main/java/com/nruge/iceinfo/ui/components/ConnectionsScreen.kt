package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.nruge.iceinfo.util.formatRemainingTimeUntil
import java.time.LocalTime
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.ConnectingTrain
import com.nruge.iceinfo.model.Departure
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.ui.theme.onSuccessContainer
import com.nruge.iceinfo.ui.theme.onWarningContainer
import com.nruge.iceinfo.ui.theme.rainbowColor

@Composable
fun ConnectionsScreen(
    status: TrainStatus,
    connections: List<ConnectingTrain>,
    departures: List<Departure> = emptyList(),
    isMockMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val targetStop = status.stops.find { it.evaNr == status.targetStopEva && !it.passed }
    val stationName = targetStop?.name ?: status.nextStop

    val missed = connections.filter { !it.reachable }
    val tight = connections.filter { it.reachable && it.transferMinutes != null && it.transferMinutes < 5 }
    val reachable = connections.filter { it.reachable && (it.transferMinutes == null || it.transferMinutes >= 5) }

    // Live-Betrieb: Das Bordportal liefert keine Anschlüsse mehr — dann wird die
    // Abfahrtstafel (ab Ankunft am gewählten Halt) selbst nach Umstiegszeit gruppiert.
    val useDepartureBoard = connections.isEmpty()
    val arrivalMs = (targetStop ?: status.stops.firstOrNull { !it.passed })?.effectiveArrivalMs ?: 0L
    val boardEntries = if (useDepartureBoard) departures.map { dep ->
        val transfer = if (arrivalMs > 0L && dep.plannedMs > 0L)
            ((dep.plannedMs + dep.delayMinutes * 60_000L - arrivalMs) / 60_000L).toInt()
        else null
        dep to transfer
    } else emptyList()
    val hasTransferInfo = boardEntries.any { it.second != null }
    val depMissed = boardEntries.filter { (dep, t) -> dep.cancelled || (t != null && t < 0) }
    val depTight = boardEntries.filter { (dep, t) -> !dep.cancelled && t != null && t in 0..4 }
    val depReachable = boardEntries.filter { (dep, t) -> !dep.cancelled && (t == null || t >= 5) }

    var showRelative by remember { mutableStateOf(false) }
    val referenceTime = if (isMockMode) LocalTime.of(8, 30) else LocalTime.now()
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            showRelative = !showRelative
        }
    }

    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Station header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.connections_header, stationName),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                val stop = targetStop ?: status.stops.firstOrNull { !it.passed }
                if (stop != null && stop.scheduledArrival.isNotEmpty()) {
                    val isDelayed = stop.delayMinutes > 0
                    val displayTime = stop.actualArrival.ifEmpty { stop.scheduledArrival }
                    val relativeArrival = formatRemainingTimeUntil(stop.scheduledArrival, stop.delayMinutes, referenceTime)
                        .takeIf { it != "--" }?.let { "in $it" }
                    val headerRelAlpha by animateFloatAsState(
                        targetValue = if (showRelative && relativeArrival != null) 1f else 0f,
                        animationSpec = tween(350),
                        label = "arrival_header_alpha"
                    )
                    Box(contentAlignment = Alignment.CenterStart) {
                        Row(
                            modifier = Modifier.alpha(1f - headerRelAlpha),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.connections_arrival, stop.scheduledArrival),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDelayed)
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = if (isDelayed) TextDecoration.LineThrough else TextDecoration.None
                            )
                            if (isDelayed) {
                                Text(
                                    text = displayTime,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (stop.delayMinutes < 0) rainbowColor()
                                    else if (stop.delayMinutes >= 5) MaterialTheme.colorScheme.error
                                    else onSuccessContainer()
                                )
                            }
                        }
                        Text(
                            text = relativeArrival ?: "",
                            modifier = Modifier.alpha(headerRelAlpha),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                stop.delayMinutes < 0  -> rainbowColor()
                                stop.delayMinutes >= 5 -> MaterialTheme.colorScheme.error
                                else                   -> onSuccessContainer()
                            }
                        )
                    }
                }
            }
        }

        if (connections.isEmpty() && departures.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.connections_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (reachable.isNotEmpty()) {
            stickyHeader(key = "header_reachable") {
                StickyConnectionHeader(listState, "header_reachable") {
                    ConnectionSectionHeader(Icons.Default.CheckCircle, stringResource(R.string.connections_section_reachable), onSuccessContainer())
                }
            }
            item(key = "group_reachable") {
                ConnectionGroup(reachable) { conn -> ConnectionCardContent(conn, showRelative, referenceTime) }
            }
        }

        if (tight.isNotEmpty()) {
            stickyHeader(key = "header_tight") {
                StickyConnectionHeader(listState, "header_tight") {
                    ConnectionSectionHeader(Icons.Default.Warning, stringResource(R.string.connections_section_tight), onWarningContainer())
                }
            }
            item(key = "group_tight") {
                ConnectionGroup(tight) { conn -> ConnectionCardContent(conn, showRelative, referenceTime) }
            }
        }

        if (missed.isNotEmpty()) {
            stickyHeader(key = "header_missed") {
                StickyConnectionHeader(listState, "header_missed") {
                    ConnectionSectionHeader(Icons.Default.Cancel, stringResource(R.string.connections_section_missed), MaterialTheme.colorScheme.error)
                }
            }
            item(key = "group_missed") {
                ConnectionGroup(missed) { conn -> ConnectionCardContent(conn, showRelative, referenceTime) }
            }
        }

        if (!useDepartureBoard && departures.isNotEmpty()) {
            stickyHeader(key = "header_departures") {
                StickyConnectionHeader(listState, "header_departures") {
                    ConnectionSectionHeader(Icons.Default.DirectionsTransit, stringResource(R.string.connections_section_departures))
                }
            }
            item(key = "group_departures") {
                ConnectionGroup(departures) { dep -> DepartureCardContent(dep, showRelative, referenceTime) }
            }
        }

        if (useDepartureBoard && boardEntries.isNotEmpty()) {
            if (!hasTransferInfo) {
                // Keine Ankunfts-/Abfahrtszeiten in Epoch-Form → flache Abfahrtstafel
                stickyHeader(key = "header_dep_board") {
                    StickyConnectionHeader(listState, "header_dep_board") {
                        ConnectionSectionHeader(Icons.Default.DirectionsTransit, stringResource(R.string.connections_section_departures_board))
                    }
                }
                item(key = "group_dep_board") {
                    ConnectionGroup(departures) { dep -> DepartureCardContent(dep, showRelative, referenceTime) }
                }
            } else {
                if (depReachable.isNotEmpty()) {
                    stickyHeader(key = "header_dep_reachable") {
                        StickyConnectionHeader(listState, "header_dep_reachable") {
                            ConnectionSectionHeader(Icons.Default.CheckCircle, stringResource(R.string.connections_section_reachable), onSuccessContainer())
                        }
                    }
                    item(key = "group_dep_reachable") {
                        ConnectionGroup(depReachable) { (dep, transfer) ->
                            DepartureCardContent(dep, showRelative, referenceTime, transferMinutes = transfer)
                        }
                    }
                }
                if (depTight.isNotEmpty()) {
                    stickyHeader(key = "header_dep_tight") {
                        StickyConnectionHeader(listState, "header_dep_tight") {
                            ConnectionSectionHeader(Icons.Default.Warning, stringResource(R.string.connections_section_tight), onWarningContainer())
                        }
                    }
                    item(key = "group_dep_tight") {
                        ConnectionGroup(depTight) { (dep, transfer) ->
                            DepartureCardContent(dep, showRelative, referenceTime, transferMinutes = transfer)
                        }
                    }
                }
                if (depMissed.isNotEmpty()) {
                    stickyHeader(key = "header_dep_missed") {
                        StickyConnectionHeader(listState, "header_dep_missed") {
                            ConnectionSectionHeader(Icons.Default.Cancel, stringResource(R.string.connections_section_missed), MaterialTheme.colorScheme.error)
                        }
                    }
                    item(key = "group_dep_missed") {
                        ConnectionGroup(depMissed) { (dep, transfer) ->
                            DepartureCardContent(dep, showRelative, referenceTime, transferMinutes = transfer)
                        }
                    }
                }
            }
        }
    }
    if (isScrolled) HorizontalDivider()
    } // Box
}

@Composable
private fun ConnectionSectionHeader(
    icon: ImageVector,
    title: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StickyConnectionHeader(
    listState: LazyListState,
    headerKey: String,
    content: @Composable () -> Unit
) {
    val isStuck by remember(headerKey) {
        derivedStateOf {
            val idx = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == headerKey }?.index ?: return@derivedStateOf false
            listState.firstVisibleItemIndex > idx
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isStuck) MaterialTheme.colorScheme.surfaceContainer
                else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0f)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        content()
    }
}

@Composable
private fun <T> ConnectionGroup(
    items: List<T>,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    content: @Composable (T) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            items.forEachIndexed { index, item ->
                content(item)
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
