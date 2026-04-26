package com.nruge.iceinfo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Train
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.SyncAlt

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home : Screen("home", Icons.Default.Train, "Status")
    object Stops : Screen("stops", Icons.AutoMirrored.Filled.List, "Halte")
    object Map : Screen("map", Icons.Default.Map, "Karte")
    object Service : Screen("service", Icons.Default.Restaurant, "Service")
    object Connections : Screen("connections", Icons.Default.SyncAlt, "Anschlüsse")
}

val navigationItems = listOf(
    Screen.Home,
    Screen.Stops,
    Screen.Map,
    Screen.Service,
    Screen.Connections
)
