package com.nruge.iceinfo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    isMockMode: Boolean,
    isConnected: Boolean,
    serviceRunning: Boolean,
    onToggleService: () -> Unit,
    onExitDemo: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onShowInfo: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.app_title),
                    fontWeight = FontWeight.Bold
                )
                when {
                    isMockMode -> Text(
                        text = stringResource(R.string.status_demo),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic
                    )
                    isConnected -> Text(
                        text = stringResource(R.string.status_live),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        },
        navigationIcon = {
            if (isMockMode) {
                IconButton(onClick = onExitDemo) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.demo_end)
                    )
                }
            }
        },
        actions = {
            if (isConnected || isMockMode) {
                IconButton(onClick = onToggleService) {
                    Icon(
                        imageVector = if (serviceRunning) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.notifications_cd),
                        tint = if (serviceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }


            var menuExpanded by remember { mutableStateOf(false) }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_cd))
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.theme_system)) },
                    onClick = { onThemeChange(AppTheme.SYSTEM); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.SettingsBrightness, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.theme_light)) },
                    onClick = { onThemeChange(AppTheme.LIGHT); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.LightMode, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.theme_dark)) },
                    onClick = { onThemeChange(AppTheme.DARK); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.DarkMode, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_info)) },
                    onClick = { onShowInfo(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.Info, null) }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun AppBottomBar(
    currentRoute: String?,
    enabled: Boolean,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        navigationItems.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                enabled = enabled,
                onClick = { onNavigate(screen.route) },
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(screen.label) }
            )
        }
    }
}
