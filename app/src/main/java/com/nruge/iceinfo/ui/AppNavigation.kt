package com.nruge.iceinfo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nruge.iceinfo.model.ConnectingTrain
import com.nruge.iceinfo.model.PoiItem
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.ui.components.ConnectionsScreen
import com.nruge.iceinfo.ui.components.HomeScreen
import com.nruge.iceinfo.ui.components.MapScreen
import com.nruge.iceinfo.ui.components.ServiceScreen
import com.nruge.iceinfo.ui.components.StopsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    innerPadding: PaddingValues,
    trainStatus: TrainStatus,
    pois: List<PoiItem>,
    connections: List<ConnectingTrain>,
    isDarkTheme: Boolean,
    isMockMode: Boolean,
    demoSpeed: Int,
    showDemoSpeed: Boolean,
    onDemoSpeedChange: (Int) -> Unit,
    onTargetStopChange: (String?) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                status = if (isMockMode) trainStatus.copy(speed = demoSpeed) else trainStatus,
                isDarkTheme = isDarkTheme,
                isMockMode = isMockMode,
                demoSpeed = demoSpeed,
                showDemoSpeed = showDemoSpeed,
                onDemoSpeedChange = onDemoSpeedChange,
                onTargetStopChange = onTargetStopChange
            )
        }
        composable(Screen.Stops.route) {
            StopsScreen(status = trainStatus, pois = pois)
        }
        composable(Screen.Map.route) {
            MapScreen(status = trainStatus)
        }
        composable(Screen.Service.route) {
            ServiceScreen(status = trainStatus)
        }
        composable(Screen.Connections.route) {
            ConnectionsScreen(status = trainStatus, connections = connections)
        }
    }
}
