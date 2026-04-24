package com.nruge.iceinfo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nruge.iceinfo.ui.MainViewModel
import com.nruge.iceinfo.ui.*
import com.nruge.iceinfo.ui.theme.*
import com.nruge.iceinfo.model.*
import com.nruge.iceinfo.ui.components.*

// -------------------------------------------------------------------------
// Activity
// -------------------------------------------------------------------------

class MainActivity : ComponentActivity() {

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startForegroundService(Intent(this, IceNotificationService::class.java))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            var demoSpeed by remember { mutableIntStateOf(114) }
            var appTheme by remember { mutableStateOf(AppTheme.SYSTEM) }
            val isDark = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            ICEInfoTheme(darkTheme = isDark) {
                val viewModel: MainViewModel = viewModel()
                val trainStatus by viewModel.trainStatus.collectAsState()
                val pois by viewModel.pois.collectAsState()
                val isMockMode by viewModel.isMockMode.collectAsState()
                val isChecking by viewModel.isChecking.collectAsState()
                val connections by viewModel.connections.collectAsState()

                val view = LocalView.current
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                }

                var serviceRunning by remember { mutableStateOf(false) }
                var showInfo by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "ICEinfo Live",
                                        fontWeight = FontWeight.Bold
                                    )
                                    when {
                                        isMockMode -> Text(
                                            text = "Demo Modus",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                            fontStyle = FontStyle.Italic
                                        )
                                        trainStatus.isConnected -> Text(
                                            text = "🟢 Live",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                if (isMockMode) {
                                    IconButton(onClick = {
                                        viewModel.setMockMode(false)
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Demo beenden")
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    if (serviceRunning) {
                                        stopService(Intent(context, IceNotificationService::class.java))
                                        serviceRunning = false
                                    } else {
                                        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                                            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            context.startForegroundService(Intent(context, IceNotificationService::class.java))
                                            serviceRunning = true
                                        } else {
                                            (context as MainActivity).requestPermissionLauncher.launch(
                                                android.Manifest.permission.POST_NOTIFICATIONS
                                            )
                                            serviceRunning = true
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (serviceRunning) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                        contentDescription = "Benachrichtigung",
                                        tint = if (serviceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                var menuExpanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menü")
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("System Standard") },
                                        onClick = { appTheme = AppTheme.SYSTEM; menuExpanded = false },
                                        leadingIcon = { Icon(Icons.Default.SettingsBrightness, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Helles Theme") },
                                        onClick = { appTheme = AppTheme.LIGHT; menuExpanded = false },
                                        leadingIcon = { Icon(Icons.Default.LightMode, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Dunkles Theme") },
                                        onClick = { appTheme = AppTheme.DARK; menuExpanded = false },
                                        leadingIcon = { Icon(Icons.Default.DarkMode, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Informationen") },
                                        onClick = { showInfo = true; menuExpanded = false },
                                        leadingIcon = { Icon(Icons.Default.Info, null) }
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            navigationItems.forEach { screen ->
                                NavigationBarItem(
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    enabled = trainStatus.isConnected || isMockMode,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(screen.icon, contentDescription = null) },
                                    label = { Text(screen.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    if (!trainStatus.isConnected && !isMockMode) {
                        NoWifiScreen(
                            modifier = Modifier.padding(innerPadding),
                            onRetry = {
                                viewModel.retryConnection()
                            },
                            onMockMode = {
                                viewModel.setMockMode(true)
                            }
                        )
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Home.route) {
                                MainScreen(
                                    status = if (isMockMode) trainStatus.copy(speed = demoSpeed) else trainStatus,
                                    isDarkTheme = isDark,
                                    isMockMode = isMockMode,
                                    demoSpeed = demoSpeed,
                                    onDemoSpeedChange = { demoSpeed = it }
                                )
                            }
                            composable(Screen.Stops.route) {
                                StopsScreen(
                                    status = trainStatus,
                                    pois = pois
                                )
                            }
                            composable(Screen.Map.route) {
                                MapScreen(status = trainStatus)
                            }
                            composable(Screen.Connections.route) {
                                ConnectionsScreen(
                                    status = trainStatus,
                                    connections = connections
                                )
                            }
                        }
                    }
                }
                if (showInfo) {
                    AlertDialog(
                        onDismissRequest = { showInfo = false },
                        icon = { Icon(Icons.Default.Train, contentDescription = null) },
                        title = { Text("ICEinfo Live", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Version 1.0", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary)
                                HorizontalDivider()
                                Text("Diese App liest Live-Daten aus der ICE Portal API des Deutsche Bahn ICE-WLANs aus und zeigt sie übersichtlich an.",
                                    style = MaterialTheme.typography.bodyMedium)
                                HorizontalDivider()
                                Text("Datenschutz", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge)
                                Text("Diese App speichert oder erfasst keine personenbezogenen Daten. Die API Abfragen erfolgen ausschließlich an die Interne API im Zug-WLAN.")
                                HorizontalDivider()
                                Text("API", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge)
                                Text("iceportal.de/api1/rs/",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary)
                                HorizontalDivider()
                                Text("Entwickelt mit", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge)
                                Text("Kotlin · Jetpack Compose · OSMDroid · Material 3",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary)
                                HorizontalDivider()
                                Text("Rechtliches", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge)
                                Text("Dies ist eine inoffizielle App. Für die Richtigkeit der angezeigten Daten kann keine Haftung übernommen werden.")
                                Text("Diese App steht in keiner Verbindung mit der DB Fernverkehr AG, DB Systel GmbH oder anderen Tochterunternehmen der Deutsche Bahn AG.")
                                Text("Diese App steht in keiner Verbindung mit der Siemens Mobility GmbH")
                                Text("Das ICEportal.de und die ICEportal API werden von der DB Systel GmbH bereitgestellt.")
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showInfo = false }) {
                                Text("Schließen")
                            }
                        }
                    )
                }
            }
        }
    }
}
