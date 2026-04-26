package com.nruge.iceinfo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.sampleTrainStatus
import com.nruge.iceinfo.ui.components.*
import com.nruge.iceinfo.ui.theme.ICEInfoTheme

/**
 * Ein Screen zum Testen aller UI-Elemente und Theme-Farben.
 * Dieser Screen ist nicht Teil der normalen App-Navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeTestScreen(isDarkTheme: Boolean) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme & UI Gallery") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- App Specific Components ---
            SectionTitle("App Components")
            TrainHeader(status = sampleTrainStatus)
            TravelSummaryCard(status = sampleTrainStatus)
            ConnectivityRow(status = sampleTrainStatus, isDarkTheme = isDarkTheme)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DelayBadge(delayMinutes = 5)
                DelayBadge(delayMinutes = 15)
                DelayBadge(delayMinutes = 0)
            }

            // --- Material 3 Buttons ---
            SectionTitle("Buttons")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { }) { Text("Filled") }
                ElevatedButton(onClick = { }) { Text("Elevated") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = { }) { Text("Tonal") }
                OutlinedButton(onClick = { }) { Text("Outlined") }
                TextButton(onClick = { }) { Text("Text") }
            }

            // --- Material 3 Cards ---
            SectionTitle("Cards")
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(16.dp)) { Text("Standard Card") }
            }
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(16.dp)) { Text("Elevated Card") }
            }
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(16.dp)) { Text("Outlined Card") }
            }

            // --- Selection Controls ---
            SectionTitle("Selection Controls")
            Row(verticalAlignment = Alignment.CenterVertically) {
                var checked by remember { mutableStateOf(true) }
                Checkbox(checked = checked, onCheckedChange = { checked = it })
                Text("Checkbox")
                Spacer(Modifier.width(16.dp))
                var switched by remember { mutableStateOf(true) }
                Switch(checked = switched, onCheckedChange = { switched = it })
                Text("Switch")
            }

            // --- Text Fields ---
            SectionTitle("Input")
            var text by remember { mutableStateOf("") }
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("TextField") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("OutlinedTextField") },
                modifier = Modifier.fillMaxWidth()
            )

            // --- Typography ---
            SectionTitle("Typography")
            Text("Display Large", style = MaterialTheme.typography.displayLarge)
            Text("Headline Medium", style = MaterialTheme.typography.headlineMedium)
            Text("Title Medium", style = MaterialTheme.typography.titleMedium)
            Text("Body Large", style = MaterialTheme.typography.bodyLarge)
            Text("Label Small", style = MaterialTheme.typography.labelSmall)
            
            // --- Color Palette ---
            SectionTitle("Color Palette")
            ColorBox("Primary", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
            ColorBox("PrimaryContainer", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            ColorBox("Secondary", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
            ColorBox("SecondaryContainer", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
            ColorBox("Tertiary", MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
            ColorBox("Background", MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground)
            ColorBox("Surface", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
            ColorBox("Error", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ColorBox(label: String, color: androidx.compose.ui.graphics.Color, onColor: androidx.compose.ui.graphics.Color) {
    Surface(
        color = color,
        contentColor = onColor,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Preview(showBackground = true, name = "Light Theme")
@Composable
fun ThemeTestScreenPreview() {
    ICEInfoTheme(darkTheme = false) {
        ThemeTestScreen(isDarkTheme = false)
    }
}

@Preview(showBackground = true, name = "Dark Theme", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ThemeTestScreenDarkPreview() {
    ICEInfoTheme(darkTheme = true) {
        ThemeTestScreen(isDarkTheme = true)
    }
}
