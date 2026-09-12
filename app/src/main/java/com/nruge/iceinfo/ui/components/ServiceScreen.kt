package com.nruge.iceinfo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.Escalator
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nruge.iceinfo.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.model.FacilityStatus
import com.nruge.iceinfo.model.FacilityType
import com.nruge.iceinfo.model.StationFacility
import com.nruge.iceinfo.model.StationInfo
import com.nruge.iceinfo.model.StationSearchResult
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.model.TrainStop
import com.nruge.iceinfo.ui.theme.onSuccessContainer
import com.nruge.iceinfo.ui.theme.successContainer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServiceScreen(
    status: TrainStatus,
    serviceStation: StationInfo?,
    searchResults: List<StationSearchResult>,
    onSearchQueryChange: (String) -> Unit,
    onStationSelect: (StationSearchResult) -> Unit,
    onLoadTrainStation: (evaNr: String, name: String) -> Unit,
    searchFabClicks: Int = 0
) {
    val targetStop = status.stops.find { it.evaNr == status.targetStopEva && !it.passed }
    var searchOpen by remember { mutableStateOf(false) }

    // Such-FAB liegt fix über dem Pager (siehe AppNavigation);
    // handledFabClicks verhindert erneutes Feuern nach Pager-Recreation
    var handledFabClicks by rememberSaveable { mutableIntStateOf(searchFabClicks) }
    LaunchedEffect(searchFabClicks) {
        if (searchFabClicks > handledFabClicks) {
            handledFabClicks = searchFabClicks
            searchOpen = true
        }
    }

    LaunchedEffect(targetStop?.evaNr) {
        if (targetStop != null && serviceStation == null) {
            onLoadTrainStation(targetStop.evaNr, targetStop.name)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        when {
            serviceStation == null -> ServiceEmptyState(
                targetStop = targetStop,
                onLoadTrainStation = onLoadTrainStation
            )
            serviceStation.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            serviceStation.error != null -> ServiceErrorState(message = serviceStation.error)
            else -> StationFacilitiesContent(station = serviceStation)
        }

        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SearchOverlay(
                searchResults = searchResults,
                onSearchQueryChange = onSearchQueryChange,
                onStationSelect = { result ->
                    onStationSelect(result)
                    onSearchQueryChange("")
                    searchOpen = false
                },
                onDismiss = {
                    onSearchQueryChange("")
                    searchOpen = false
                }
            )
        }
    }
}

@Composable
private fun SearchOverlay(
    searchResults: List<StationSearchResult>,
    onSearchQueryChange: (String) -> Unit,
    onStationSelect: (StationSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(50)
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss(); query = "" }
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearchQueryChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.service_search_station)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = {
                                query = ""
                                onSearchQueryChange("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.service_clear))
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (searchResults.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Column {
                            searchResults.forEachIndexed { index, result ->
                                ListItem(
                                    headlineContent = { Text(result.name) },
                                    leadingContent = {
                                        Icon(Icons.Default.Train, contentDescription = null)
                                    },
                                    modifier = Modifier.clickable {
                                        onStationSelect(result)
                                        query = ""
                                    }
                                )
                                if (index < searchResults.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceEmptyState(
    targetStop: TrainStop?,
    onLoadTrainStation: (evaNr: String, name: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.service_select_station_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.service_select_station_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (targetStop != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onLoadTrainStation(targetStop.evaNr, targetStop.name) },
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(targetStop.name)
            }
        }
    }
}

@Composable
private fun ServiceErrorState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StationFacilitiesContent(station: StationInfo) {
    val groupedLive = station.liveFacilities
        .groupBy { it.type }
        .entries
        .sortedBy { it.key.ordinal }

    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.service_station_title, station.name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        groupedLive.forEach { (type, facilities) ->
            stickyHeader(key = "header_${type.name}") {
                StickyServiceHeader(listState, "header_${type.name}") {
                    FacilitySectionHeader(icon = type.icon(), title = type.label())
                }
            }
            item(key = type.name) {
                FacilityTypeGroup(type = type, facilities = facilities, showHeader = false)
            }
        }

        if (station.staticFacilities.isNotEmpty()) {
            stickyHeader(key = "static_header") {
                StickyServiceHeader(listState, "static_header") {
                    FacilitySectionHeader(title = stringResource(R.string.service_facilities))
                }
            }
            item(key = "static") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    station.staticFacilities.forEach { type ->
                        StaticFacilityChip(type = type)
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.service_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
    if (isScrolled) HorizontalDivider()
    } // Box
}

@Composable
private fun StickyServiceHeader(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacilityTypeGroup(type: FacilityType, facilities: List<StationFacility>, showHeader: Boolean = true) {
    val active = facilities.filter { it.status == FacilityStatus.ACTIVE }
    val broken = facilities.filter { it.status != FacilityStatus.ACTIVE }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showHeader) FacilitySectionHeader(icon = type.icon(), title = type.label())

        if (active.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                active.forEach { facility ->
                    Surface(
                        shape = CircleShape,
                        color = successContainer()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = type.icon(),
                                contentDescription = null,
                                tint = onSuccessContainer(),
                                modifier = Modifier.size(18.dp)
                            )
                            if (facility.label.isNotBlank()) {
                                Text(
                                    text = facility.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = onSuccessContainer()
                                )
                            }
                        }
                    }
                }
            }
        }

        broken.forEach { facility ->
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = type.icon(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = facility.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (facility.description.isNotEmpty()) {
                            Text(
                                text = facility.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = stringResource(R.string.service_out_of_order),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaticFacilityChip(type: FacilityType) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = type.icon(),
                contentDescription = type.label(),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = type.label(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun FacilitySectionHeader(title: String, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun FacilityType.icon(): ImageVector = when (this) {
    FacilityType.ELEVATOR  -> Icons.Default.Elevator
    FacilityType.ESCALATOR -> Icons.Default.Escalator
    FacilityType.TOILET    -> Icons.Default.Wc
    FacilityType.WIFI      -> Icons.Default.Wifi
    FacilityType.INFO_DESK -> Icons.Default.SupportAgent
    FacilityType.DEPARTURE_MONITOR -> Icons.Default.Schedule
    FacilityType.RAMP      -> Icons.AutoMirrored.Filled.Accessible
    FacilityType.PARKING   -> Icons.Default.LocalParking
    FacilityType.BIKE_PARKING -> Icons.Default.PedalBike
    FacilityType.WAITING_ROOM -> Icons.Default.Chair
}

@Composable
private fun FacilityType.label(): String = when (this) {
    FacilityType.ELEVATOR         -> stringResource(R.string.facility_elevator)
    FacilityType.ESCALATOR        -> stringResource(R.string.facility_escalator)
    FacilityType.TOILET           -> stringResource(R.string.facility_toilet)
    FacilityType.WIFI             -> stringResource(R.string.facility_wifi)
    FacilityType.INFO_DESK        -> stringResource(R.string.facility_info_desk)
    FacilityType.DEPARTURE_MONITOR -> stringResource(R.string.facility_departure_monitor)
    FacilityType.RAMP             -> stringResource(R.string.facility_ramp)
    FacilityType.PARKING          -> stringResource(R.string.facility_parking)
    FacilityType.BIKE_PARKING     -> stringResource(R.string.facility_bike_parking)
    FacilityType.WAITING_ROOM     -> stringResource(R.string.facility_waiting_room)
}
