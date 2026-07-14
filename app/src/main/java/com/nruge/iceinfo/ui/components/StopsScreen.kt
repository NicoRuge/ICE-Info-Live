package com.nruge.iceinfo.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import com.nruge.iceinfo.util.formatRemainingTimeUntil
import kotlinx.coroutines.delay
import java.time.LocalTime
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.OsmTrackData
import com.nruge.iceinfo.model.PoiItem
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.model.TrainStop
import com.nruge.iceinfo.ui.theme.onSuccessContainer
import com.nruge.iceinfo.ui.theme.rainbowColor

@Composable
fun TimelineStopRow(stop: TrainStop, isFirst: Boolean, isLast: Boolean, showRelative: Boolean = false, referenceTime: LocalTime = LocalTime.now()) {
    val isPassed = stop.passed
    val isNext = stop.isNext

    val travelledLine = MaterialTheme.colorScheme.primary
    val pendingLine = MaterialTheme.colorScheme.outlineVariant

    val rowBackground = if (isNext)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    else
        Color.Transparent

    val isCancelled = stop.isCancelled

    val nameColor = when {
        isCancelled -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        isPassed    -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        isNext      -> MaterialTheme.colorScheme.onPrimaryContainer
        else        -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Times + delay column LEFT of timeline
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 4.dp, top = if (isNext) 12.dp else 8.dp, bottom = if (isNext) 12.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 2×2 time grid
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
            ) {
                if (stop.scheduledArrival.isNotEmpty()) {
                    StopTimePair(
                        scheduled = stop.scheduledArrival,
                        actual = stop.actualArrival,
                        delay = stop.delayMinutes,
                        isPassed = isPassed,
                        isNext = isNext,
                        isCancelled = isCancelled,
                        showRelative = showRelative,
                        referenceTime = referenceTime
                    )
                }
                if (stop.scheduledDeparture.isNotEmpty()) {
                    StopTimePair(
                        scheduled = stop.scheduledDeparture,
                        actual = stop.actualDeparture,
                        delay = stop.departureDelayMinutes,
                        isPassed = isPassed,
                        isNext = isNext,
                        isCancelled = isCancelled,
                        showRelative = showRelative,
                        referenceTime = referenceTime
                    )
                }
            }

            // Delay column — zentriert neben dem 2×2 Block
            Column(
                modifier = Modifier
                    .width(34.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!isPassed && !isCancelled) {
                    if (stop.scheduledArrival.isNotEmpty() && stop.delayMinutes != 0) {
                        StopDelayLabel(stop.delayMinutes)
                    }
                    val depDiffersFromArr = stop.departureDelayMinutes != stop.delayMinutes
                    if (stop.scheduledDeparture.isNotEmpty() && stop.departureDelayMinutes != 0 && depDiffersFromArr) {
                        StopDelayLabel(stop.departureDelayMinutes)
                    }
                }
            }
        }

        // Timeline column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(
                        when {
                            isFirst -> Color.Transparent
                            isPassed || isNext -> travelledLine
                            else -> pendingLine
                        }
                    )
            )
            when {
                isCancelled -> Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
                isNext -> Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Train,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                isPassed -> Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(travelledLine)
                )
                else -> Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(
                        when {
                            isLast -> Color.Transparent
                            isPassed -> travelledLine
                            else -> pendingLine
                        }
                    )
            )
        }

        // Station info RIGHT of timeline
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(rowBackground)
                .padding(
                    vertical = if (isNext) 12.dp else 8.dp,
                    horizontal = 10.dp
                )
        ) {
            Text(
                text = stop.name,
                color = nameColor,
                style = if (isNext) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyMedium,
                fontWeight = when {
                    isNext -> FontWeight.Bold
                    isPassed -> FontWeight.Normal
                    else -> FontWeight.Medium
                },
                textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
            )
            if (isCancelled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.stop_cancelled),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (!isCancelled && stop.track.isNotEmpty()) {
                TrackLabel(
                    text = stringResource(R.string.track_full, stop.track),
                    changed = stop.trackChanged,
                    style = MaterialTheme.typography.bodySmall
                ) {
                    Text(
                        text = stringResource(R.string.track_full, stop.track),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = if (isPassed) 0.5f else 1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (stop.isAdditional) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = onSuccessContainer()
                            .copy(alpha = if (isPassed) 0.5f else 1f)
                    )
                    Text(
                        text = stringResource(R.string.stop_additional),
                        color = onSuccessContainer()
                            .copy(alpha = if (isPassed) 0.5f else 1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (!isCancelled && stop.directionChange) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UTurnLeft,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.primary
                            .copy(alpha = if (isPassed) 0.5f else 1f)
                    )
                    Text(
                        text = stringResource(R.string.stop_direction_change),
                        color = MaterialTheme.colorScheme.primary
                            .copy(alpha = if (isPassed) 0.5f else 1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StopDelayLabel(delay: Int) {
    if (delay < 0) {
        Text(
            text = "($delay)",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = rainbowColor()
        )
    } else {
        Text(
            text = "(+$delay)",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = if (delay >= 5) MaterialTheme.colorScheme.error else onSuccessContainer()
        )
    }
}

@Composable
private fun StopTimePair(
    scheduled: String,
    actual: String,
    delay: Int,
    isPassed: Boolean,
    isNext: Boolean,
    isCancelled: Boolean = false,
    showRelative: Boolean = false,
    referenceTime: LocalTime = LocalTime.now()
) {
    val isEarly = delay < 0 && !isPassed && !isCancelled && actual.isNotEmpty()
    val isDelayed = delay > 0 && !isPassed && !isCancelled && actual.isNotEmpty()
    val displayActual = actual.ifEmpty { scheduled }
    val canShowRelative = showRelative && !isPassed && !isCancelled
    val relativeText = if (canShowRelative) {
        val remaining = formatRemainingTimeUntil(scheduled, delay, referenceTime)
        if (remaining != "--") "in $remaining" else null
    } else null

    val relAlpha by animateFloatAsState(
        targetValue = if (relativeText != null) 1f else 0f,
        animationSpec = tween(350),
        label = "time_rel"
    )

    // Keep the last non-null relativeText so the fade-out still renders the old string
    var lastRelativeText by remember { mutableStateOf(relativeText) }
    if (relativeText != null) lastRelativeText = relativeText

    // Box keeps both states always laid out — only alpha changes, layout never moves
    Box(contentAlignment = Alignment.CenterEnd) {
        Row(
            modifier = Modifier.alpha(1f - relAlpha),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = scheduled,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = if (isNext && !isDelayed && !isEarly && !isCancelled) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isCancelled          -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    isDelayed || isEarly -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    isPassed             -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else                 -> MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if ((isDelayed || isEarly || isCancelled) && scheduled.isNotEmpty()) TextDecoration.LineThrough else TextDecoration.None
            )
            if (isEarly) {
                Text(
                    text = displayActual,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = rainbowColor()
                )
            } else {
                Text(
                    text = displayActual,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = if (isDelayed || isNext) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isCancelled             -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        isPassed                -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        isDelayed && delay >= 5 -> MaterialTheme.colorScheme.error
                        else                    -> onSuccessContainer()
                    },
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
                )
            }
        }
        Text(
            text = lastRelativeText ?: "",
            modifier = Modifier.alpha(relAlpha),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = when {
                isDelayed && delay >= 5 -> MaterialTheme.colorScheme.error
                isEarly                 -> rainbowColor()
                else                    -> onSuccessContainer()
            }
        )
    }
}
@Composable
fun JourneyScreen(
    status: TrainStatus,
    osmData: OsmTrackData,
    pois: List<PoiItem>,
    isMockMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showRelative by remember { mutableStateOf(false) }
    val referenceTime = if (isMockMode) LocalTime.of(8, 30) else LocalTime.now()
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            showRelative = !showRelative
        }
    }

    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            stickyHeader(key = "header_stops") {
                StickyStopsHeader(listState, "header_stops") {
                    TrackSectionHeader(
                        icon = Icons.AutoMirrored.Filled.List,
                        title = stringResource(R.string.stops_title)
                    )
                }
            }
            item(key = "card_stops") {
                if (status.stops.isNotEmpty()) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            status.stops.forEachIndexed { index, stop ->
                                TimelineStopRow(
                                    stop = stop,
                                    isFirst = index == 0,
                                    isLast = index == status.stops.lastIndex,
                                    showRelative = showRelative,
                                    referenceTime = referenceTime
                                )
                            }
                        }
                    }
                } else {
                    Text(stringResource(R.string.stops_none))
                }
            }

            // Track section — only when connected and position is available
            if (status.isConnected && (status.latitude != 0.0 || status.longitude != 0.0)) {
                when {
                    osmData.isLoading -> {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    osmData.error != null -> {
                        item(key = "error") {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(osmData.error, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    else -> {
                        stickyHeader(key = "header_track") {
                            StickyStopsHeader(listState, "header_track") {
                                TrackSectionHeader(
                                    icon = Icons.Default.Train,
                                    title = stringResource(R.string.track_section_track)
                                )
                            }
                        }
                        item(key = "card_track") {
                            TrackPropertiesCard(trackInfo = osmData.trackInfo)
                        }

                        stickyHeader(key = "header_features") {
                            StickyStopsHeader(listState, "header_features") {
                                TrackSectionHeader(
                                    icon = Icons.Default.Route,
                                    title = stringResource(R.string.track_section_features)
                                )
                            }
                        }
                        item(key = "card_features") {
                            if (osmData.features.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.track_no_features),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                AppCard(modifier = Modifier.fillMaxWidth()) {
                                    osmData.features.forEachIndexed { index, feature ->
                                        FeatureRow(
                                            label = feature.type.shortLabel(),
                                            containerColor = feature.type.containerColor(),
                                            onContainerColor = feature.type.onContainerColor(),
                                            name = feature.name,
                                            distanceKm = feature.distanceKm
                                        )
                                        if (index < osmData.features.lastIndex) {
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        if (pois.isNotEmpty()) {
                            stickyHeader(key = "header_pois") {
                                StickyStopsHeader(listState, "header_pois") {
                                    TrackSectionHeader(
                                        icon = Icons.Default.Place,
                                        title = stringResource(R.string.pois_title_plain)
                                    )
                                }
                            }
                            item(key = "card_pois") {
                                AppCard(modifier = Modifier.fillMaxWidth()) {
                                    pois.forEachIndexed { index, poi ->
                                        FeatureRow(
                                            label = poi.type.poiLabel(),
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            onContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            name = poi.name,
                                            distanceKm = poi.distance / 1000.0
                                        )
                                        if (index < pois.lastIndex) {
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "source") {
                            Text(
                                text = stringResource(R.string.track_source),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        if (isScrolled) HorizontalDivider()
    }
}

@Composable
private fun StickyStopsHeader(
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
