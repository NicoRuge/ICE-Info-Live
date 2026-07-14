package com.nruge.iceinfo.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.nruge.iceinfo.ui.components.MenuScreen
import com.nruge.iceinfo.ui.components.ServiceScreen

@Composable
fun AppNavigation(
    innerPadding: PaddingValues,
    pagerState: PagerState,
    isJourneysVisible: Boolean,
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
    isRecording: Boolean,
    liveRecording: LiveRecordingState?,
    onStartRecording: () -> Unit,
    onExportJourneys: (android.net.Uri, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onImportJourneys: (android.net.Uri, (Int) -> Unit) -> Unit = { _, _ -> },
    menuItems: List<MenuCategory>,
    isMenuLoading: Boolean,
    onLoadMenu: () -> Unit,
    onRefreshMenu: () -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        beyondViewportPageCount = 0,
        key = { it },
        userScrollEnabled = !isJourneysVisible
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
            Screen.Menu -> MenuScreen(
                categories = menuItems,
                isLoading = isMenuLoading,
                trainConnected = trainStatus.isConnected || isMockMode,
                onLoad = onLoadMenu,
                onRefresh = onRefreshMenu
            )
            Screen.Service -> ServiceScreen(
                status = trainStatus,
                serviceStation = serviceStation,
                searchResults = stationSearchResults,
                onSearchQueryChange = onStationSearchQueryChange,
                onStationSelect = onStationSelect,
                onLoadTrainStation = onLoadTrainStation
            )
            Screen.Connections -> ConnectionsScreen(
                status = trainStatus,
                connections = connections,
                departures = departures,
                isMockMode = isMockMode
            )
            else -> Box(Modifier.fillMaxSize())
        }
        } // clipToBounds Box
    }

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
            isConnected = trainStatus.isConnected,
            isRecording = isRecording,
            liveRecording = liveRecording,
            onStartRecording = onStartRecording,
            onExportJourneys = onExportJourneys,
            onImportJourneys = onImportJourneys
        )
    }
}
