package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.ui.theme.*

private data class ConnectivityColors(val container: Color, val content: Color)

@Composable
private fun connectivityColors(connectivity: String, isDark: Boolean): ConnectivityColors {
    return when (connectivity) {
        "STRONG" -> ConnectivityColors(
            container = if (isDark) Green20 else Green90,
            content = if (isDark) Green90 else Green40
        )
        "WEAK" -> ConnectivityColors(
            container = if (isDark) Orange20 else Orange90,
            content = if (isDark) Orange90 else Orange40
        )
        "NO_CONNECTION" -> ConnectivityColors(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.error
        )
        else -> ConnectivityColors(
            container = if (isDark) Grey20 else Grey90,
            content = if (isDark) Grey90 else Grey40
        )
    }
}

@Composable
private fun connectivityLabel(connectivity: String): String = when (connectivity) {
    "STRONG" -> stringResource(R.string.connectivity_strong)
    "WEAK" -> stringResource(R.string.connectivity_weak)
    "NO_INFO" -> stringResource(R.string.connectivity_no_info)
    "NO_CONNECTION" -> stringResource(R.string.connectivity_none)
    else -> "—"
}

@Composable
private fun wagonClassLabel(wagonClass: String): String = when (wagonClass) {
    "FIRST" -> "1"
    "SECOND" -> "2"
    else -> "—"
}

@Composable
fun ConnectivityRow(status: TrainStatus, isDarkTheme: Boolean) {
    val colors = connectivityColors(status.connectivity, isDarkTheme)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.wagon_class_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = wagonClassLabel(status.wagonClass),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = colors.container)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.wifi_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.content
                )
                Text(
                    text = connectivityLabel(status.connectivity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.content
                )
            }
        }
    }
}
