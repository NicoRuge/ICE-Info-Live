package com.nruge.iceinfo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.sampleTrainStatus
import com.nruge.iceinfo.ui.components.*
import com.nruge.iceinfo.ui.theme.ICEInfoTheme

/**
 * Diese Datei dient als Galerie für alle UI-Komponenten und zum Testen der Theme-Farben.
 * Sie ist nicht in der App-Navigation verlinkt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeGalleryScreen(isDarkTheme: Boolean) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Theme & UI Gallery",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // --- App Specific Components ---
            SectionLabel("App Components")
            TrainHeader(status = sampleTrainStatus)
            TravelSummaryCard(status = sampleTrainStatus)
            ConnectivityRow(status = sampleTrainStatus, isDarkTheme = isDarkTheme)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DelayBadge(delayMinutes = 5)
                DelayBadge(delayMinutes = 15)
                DelayBadge(delayMinutes = 0)
            }

            // --- Material 3 Elements ---
            SectionLabel("Material 3 Elements")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { }) { Text("Primary") }
                FilledTonalButton(onClick = { }) { Text("Tonal") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { }) { Text("Outlined") }
                TextButton(onClick = { }) { Text("Text") }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Standard Card", style = MaterialTheme.typography.titleMedium)
                    Text("Using surface color for container", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // --- Color Palette ---
            SectionLabel("Color Palette")
            ColorRow("Primary", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
            ColorRow("PrimaryContainer", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            ColorRow("Secondary", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
            ColorRow("SecondaryContainer", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
            ColorRow("Tertiary", MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
            ColorRow("Background", MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground)
            ColorRow("Surface", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
            ColorRow("Error", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ColorRow(name: String, backgroundColor: Color, contentColor: Color) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Preview(showBackground = true, name = "Gallery Light")
@Composable
fun ThemeGalleryLightPreview() {
    ICEInfoTheme(darkTheme = false) {
        ThemeGalleryScreen(isDarkTheme = false)
    }
}

@Preview(showBackground = true, name = "Gallery Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ThemeGalleryDarkPreview() {
    ICEInfoTheme(darkTheme = true) {
        ThemeGalleryScreen(isDarkTheme = true)
    }
}
