package com.nruge.iceinfo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    appTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    isMockMode: Boolean,
    showDemoSpeed: Boolean,
    onToggleDemoSpeed: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Einstellungen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Erscheinungsbild",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val options = listOf(
                    Triple(AppTheme.SYSTEM, stringResource(R.string.theme_system), Icons.Default.SettingsBrightness),
                    Triple(AppTheme.LIGHT, stringResource(R.string.theme_light), Icons.Default.LightMode),
                    Triple(AppTheme.DARK, stringResource(R.string.theme_dark), Icons.Default.DarkMode)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (theme, label, icon) ->
                        SegmentedButton(
                            selected = appTheme == theme,
                            onClick = { onThemeChange(theme) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            icon = { Icon(icon, contentDescription = null) }
                        ) {
                            Text(
                                text = label,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (isMockMode) {
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Demo-Geschwindigkeit") },
                    supportingContent = { Text("Slider zum Anpassen anzeigen") },
                    leadingContent = { Icon(Icons.Default.Speed, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = showDemoSpeed,
                            onCheckedChange = onToggleDemoSpeed
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
    }
}
