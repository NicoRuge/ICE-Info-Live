package com.nruge.iceinfo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.ConnectingTrain
import com.nruge.iceinfo.model.Departure
import com.nruge.iceinfo.model.OsmTrackData
import com.nruge.iceinfo.model.PoiItem
import com.nruge.iceinfo.model.StationInfo
import com.nruge.iceinfo.model.StationSearchResult
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.model.WeatherInfo
import com.nruge.iceinfo.model.LiveRecordingState
import com.nruge.iceinfo.model.MenuCategory
import com.nruge.iceinfo.model.SavedJourney
import com.nruge.iceinfo.ui.components.ConnectionsScreen
import com.nruge.iceinfo.ui.components.HomeScreen
import com.nruge.iceinfo.ui.components.JourneyScreen
import com.nruge.iceinfo.ui.components.JourneysScreen
import com.nruge.iceinfo.ui.components.ServiceScreen

@Composable
fun AppNavigation(
    innerPadding: PaddingValues,
    pagerState: PagerState,
    isJourneysVisible: Boolean,
    isConnectionsVisible: Boolean = false,
    trainStatus: TrainStatus,
    pois: List<PoiItem>,
    connections: List<ConnectingTrain>,
    departures: List<Departure>,
    weather: WeatherInfo?,
    osmData: OsmTrackData,
    isMockMode: Boolean,
    demoSpeed: Int,
    showDemoSpeed: Boolean,
    reducedMotion: Boolean,
    onDemoSpeedChange: (Int) -> Unit,
    onTargetStopChange: (String?) -> Unit,
    coaches: List<com.nruge.iceinfo.model.Coach>,
    coachStopName: String,
    selectedCoach: Int?,
    seatNumber: String,
    onCoachChange: (Int?) -> Unit,
    onSeatChange: (String) -> Unit,
    serviceStation: StationInfo?,
    stationSearchResults: List<StationSearchResult>,
    onStationSearchQueryChange: (String) -> Unit,
    onStationSelect: (StationSearchResult) -> Unit,
    onLoadTrainStation: (evaNr: String, name: String) -> Unit,
    savedJourneys: List<SavedJourney>,
    onDeleteJourney: (String) -> Unit,
    onUpdateJourney: (SavedJourney) -> Unit = {},
    isRecording: Boolean,
    liveRecording: LiveRecordingState?,
    onStartRecording: () -> Unit,
    onExportJourneys: (android.net.Uri, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onImportJourneys: (android.net.Uri, (Int) -> Unit) -> Unit = { _, _ -> },
    menuItems: List<MenuCategory>,
    isMenuLoading: Boolean,
    onLoadMenu: () -> Unit
) {
    // Klick-Trigger für die FABs, die fix über dem Pager liegen und beim
    // Links/Rechts-Wischen nicht mitwandern (Aktion läuft im jeweiligen Screen).
    // Saveable, damit der Stand zu den "zuletzt behandelt"-Zählern in den
    // Screens passt, die Pager-Recreation und Config-Changes überleben.
    var menuFabClicks by rememberSaveable { mutableIntStateOf(0) }
    var searchFabClicks by rememberSaveable { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 0,
        key = { it },
        userScrollEnabled = !isJourneysVisible && !isConnectionsVisible
    ) { page ->
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        when (navigationItems.getOrNull(page)) {
            Screen.Home -> {
                if (!trainStatus.isConnected && !isMockMode) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer))
                } else {
                    HomeScreen(
                        status = if (isMockMode) trainStatus.copy(speed = demoSpeed) else trainStatus,
                        weather = weather,
                        isMockMode = isMockMode,
                        demoSpeed = demoSpeed,
                        showDemoSpeed = showDemoSpeed,
                        reducedMotion = reducedMotion,
                        coaches = coaches,
                        coachStopName = coachStopName,
                        selectedCoach = selectedCoach,
                        seatNumber = seatNumber,
                        menuCategories = menuItems,
                        isMenuLoading = isMenuLoading,
                        onLoadMenu = onLoadMenu,
                        menuFabClicks = menuFabClicks,
                        onDemoSpeedChange = onDemoSpeedChange,
                        onTargetStopChange = onTargetStopChange,
                        onCoachChange = onCoachChange,
                        onSeatChange = onSeatChange
                    )
                }
            }
            Screen.Stops -> JourneyScreen(
                status = trainStatus,
                osmData = osmData,
                pois = pois,
                isMockMode = isMockMode
            )
            Screen.Service -> ServiceScreen(
                status = trainStatus,
                serviceStation = serviceStation,
                searchResults = stationSearchResults,
                onSearchQueryChange = onStationSearchQueryChange,
                onStationSelect = onStationSelect,
                onLoadTrainStation = onLoadTrainStation,
                searchFabClicks = searchFabClicks
            )
            Screen.Connections -> ConnectionsScreen(
                status = trainStatus,
                connections = connections,
                departures = departures,
                isMockMode = isMockMode
            )
            Screen.Journeys -> JourneysScreen(
                journeys = savedJourneys,
                onDeleteJourney = onDeleteJourney,
                onUpdateJourney = onUpdateJourney,
                isConnected = trainStatus.isConnected,
                isRecording = isRecording,
                liveRecording = liveRecording,
                onStartRecording = onStartRecording,
                onExportJourneys = onExportJourneys,
                onImportJourneys = onImportJourneys,
                showRecordFab = false
            )
            else -> Box(Modifier.fillMaxSize())
        }
        } // clipToBounds Box
    }

    // Feste FAB-Ebene: wischt nicht mit den Pager-Seiten mit
    if (!isJourneysVisible && !isConnectionsVisible) {
        Crossfade(
            targetState = navigationItems.getOrNull(pagerState.currentPage),
            label = "pagerFab",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (screen) {
                    Screen.Home -> if (trainStatus.isConnected || isMockMode) {
                        FloatingActionButton(
                            onClick = { menuFabClicks++ },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 16.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = stringResource(R.string.nav_menu)
                            )
                        }
                    }
                    Screen.Service -> FloatingActionButton(
                        onClick = { searchFabClicks++ },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.service_search_station)
                        )
                    }
                    Screen.Journeys -> if (trainStatus.isConnected && !isRecording) {
                        ExtendedFloatingActionButton(
                            onClick = onStartRecording,
                            icon = { Icon(Icons.Default.FiberManualRecord, contentDescription = null) },
                            text = { Text(stringResource(R.string.record_fab)) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    } // Box (Pager + FAB-Ebene)

    // Overlay: Meine Fahrten — bleibt auch ohne Zug-WLAN über das Top-Bar-Menü erreichbar
    // (die Bottom-Navigation ist offline ausgeblendet).
    AnimatedVisibility(
        visible = isJourneysVisible,
        enter = if (reducedMotion) fadeIn() else
            slideInHorizontally(tween(300)) { it } + fadeIn(tween(200)),
        exit = if (reducedMotion) fadeOut() else
            slideOutHorizontally(tween(250)) { it } + fadeOut(tween(150))
    ) {
        JourneysScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(innerPadding),
            journeys = savedJourneys,
            onDeleteJourney = onDeleteJourney,
            onUpdateJourney = onUpdateJourney,
            isConnected = trainStatus.isConnected,
            isRecording = isRecording,
            liveRecording = liveRecording,
            onStartRecording = onStartRecording,
            onExportJourneys = onExportJourneys,
            onImportJourneys = onImportJourneys
        )
    }

    // Overlay: Anschlüsse — aus der Bottom-Navigation ins Top-Bar-Menü umgezogen.
    AnimatedVisibility(
        visible = isConnectionsVisible,
        enter = if (reducedMotion) fadeIn() else
            slideInHorizontally(tween(300)) { it } + fadeIn(tween(200)),
        exit = if (reducedMotion) fadeOut() else
            slideOutHorizontally(tween(250)) { it } + fadeOut(tween(150))
    ) {
        ConnectionsScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(innerPadding),
            status = trainStatus,
            connections = connections,
            departures = departures,
            isMockMode = isMockMode
        )
    }
}
