package com.nruge.iceinfo.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.AppTheme
import com.nruge.iceinfo.util.SettingsManager

/**
 * Einstellungen als vollwertige Seite (Vollbild-Overlay über das Top-Bar-Menü,
 * Zurück-Pfeil in der geteilten AppTopBar). Ehemals `SettingsSheet`
 * (ModalBottomSheet) — als Seite umgebaut, um Platz für weitere Bereiche wie die
 * Träwelling-Konto-Verbindung zu schaffen.
 */
@Composable
fun SettingsScreen(
    appTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    isMockMode: Boolean,
    showDemoSpeed: Boolean,
    onToggleDemoSpeed: (Boolean) -> Unit,
    reducedMotion: Boolean,
    onToggleReducedMotion: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    crashReportingEnabled: Boolean,
    onToggleCrashReporting: (Boolean) -> Unit,
    onDebug: () -> Unit,
    traewellingConnected: Boolean,
    traewellingUsername: String?,
    onConnectTraewelling: () -> Unit,
    onDisconnectTraewelling: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transparent = ListItemDefaults.colors(
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Konto: Verbindung zu externen Diensten (aktuell nur Träwelling, noch ohne Funktion)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_account),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_traewelling_title)) },
                supportingContent = {
                    Text(
                        if (traewellingConnected) {
                            traewellingUsername?.let { stringResource(R.string.settings_traewelling_connected_named, it) }
                                ?: stringResource(R.string.settings_traewelling_connected)
                        } else {
                            stringResource(R.string.settings_traewelling_desc)
                        }
                    )
                },
                leadingContent = { Icon(Icons.Default.Train, contentDescription = null) },
                trailingContent = {
                    if (traewellingConnected) {
                        OutlinedButton(onClick = onDisconnectTraewelling) {
                            Text(stringResource(R.string.settings_traewelling_disconnect))
                        }
                    } else {
                        FilledTonalButton(onClick = onConnectTraewelling) {
                            Text(stringResource(R.string.settings_traewelling_connect))
                        }
                    }
                },
                colors = transparent,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_appearance),
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
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        icon = { Icon(icon, contentDescription = null) }
                    ) {
                        Text(text = label, maxLines = 1)
                    }
                }
            }
        }

        if (isMockMode) {
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_demo_speed_title)) },
                supportingContent = { Text(stringResource(R.string.settings_demo_speed_desc)) },
                leadingContent = { Icon(Icons.Default.Speed, contentDescription = null) },
                trailingContent = {
                    Switch(checked = showDemoSpeed, onCheckedChange = onToggleDemoSpeed)
                },
                colors = transparent,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_language_title)) },
            supportingContent = { Text(if (language == "de") "Deutsch" else "English") },
            leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = language == "en",
                    onCheckedChange = { isEn -> onLanguageChange(if (isEn) "en" else "de") }
                )
            },
            colors = transparent,
            modifier = Modifier.align(Alignment.Start)
        )

        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_reduced_motion_title)) },
            supportingContent = { Text(stringResource(R.string.settings_reduced_motion_desc)) },
            leadingContent = { Icon(Icons.Default.Animation, contentDescription = null) },
            trailingContent = {
                Switch(checked = reducedMotion, onCheckedChange = onToggleReducedMotion)
            },
            colors = transparent,
            modifier = Modifier.align(Alignment.Start)
        )

        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_crash_reporting_title)) },
            supportingContent = { Text(stringResource(R.string.settings_crash_reporting_desc)) },
            leadingContent = { Icon(Icons.Default.ReportProblem, contentDescription = null) },
            trailingContent = {
                Switch(checked = crashReportingEnabled, onCheckedChange = onToggleCrashReporting)
            },
            colors = transparent,
            modifier = Modifier.align(Alignment.Start)
        )

        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_debug_title)) },
            supportingContent = { Text(stringResource(R.string.settings_debug_desc)) },
            leadingContent = { Icon(Icons.Default.BugReport, contentDescription = null) },
            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
            colors = transparent,
            modifier = Modifier
                .align(Alignment.Start)
                .clickable(onClick = onDebug)
        )

        // Install-ID nur anzeigen, wenn bereits eine erzeugt wurde (z.B. durch
        // Fahrten-Teilen) — die Anzeige selbst legt keine neue an.
        val context = LocalContext.current
        val installId = remember { SettingsManager.peekInstallId(context) }
        if (installId != null) {
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_install_id_title)) },
                supportingContent = { Text(installId) },
                leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                colors = transparent,
                modifier = Modifier
                    .align(Alignment.Start)
                    .clickable {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("Install-ID", installId))
                    }
            )
        }
    }
}
