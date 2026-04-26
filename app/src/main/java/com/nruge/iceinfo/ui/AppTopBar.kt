package com.nruge.iceinfo.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTopBar(
    isMockMode: Boolean,
    isConnected: Boolean,
    serviceRunning: Boolean,
    onToggleService: () -> Unit,
    onExitDemo: () -> Unit,
    onShowSettings: () -> Unit,
    onShowInfo: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                    isConnected -> Text(
                        text = stringResource(R.string.status_live),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            var menuExpanded by remember { mutableStateOf(false) }

            if (isConnected || isMockMode) {
                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.TonalLeadingButton(
                            onClick = onToggleService,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = if (serviceRunning) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.notifications_cd)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Benachrichtigung")
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.TonalTrailingButton(
                            checked = menuExpanded,
                            onCheckedChange = { menuExpanded = it },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = if (menuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.menu_cd),
                                modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize)
                            )
                        }
                    }
                )
            } else {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_cd))
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Einstellungen") },
                    onClick = { onShowSettings(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.Settings, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_info)) },
                    onClick = { onShowInfo(); menuExpanded = false },
                    leadingIcon = { Icon(Icons.Default.Info, null) }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        scrollBehavior = scrollBehavior
    )
}

@Composable
fun AppBottomBar(
    currentRoute: String?,
    enabled: Boolean,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 4.dp)
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            tonalElevation = 4.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .selectableGroup()
                    .animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navigationItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route

                    Surface(
                        onClick = { onNavigate(screen.route) },
                        enabled = enabled,
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(
                                    horizontal = if (isSelected) 14.dp else 8.dp,
                                    vertical = 12.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = screen.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
