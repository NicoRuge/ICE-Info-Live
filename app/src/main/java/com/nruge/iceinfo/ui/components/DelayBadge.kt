package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class DelayBadgeSize { SMALL, MEDIUM }

@Composable
fun DelayBadge(
    delayMinutes: Int,
    size: DelayBadgeSize = DelayBadgeSize.MEDIUM,
    showUnit: Boolean = true
) {
    if (delayMinutes <= 0) return
    val text = if (showUnit) "+$delayMinutes min" else "+$delayMinutes"
    val (hPad, vPad) = when (size) {
        DelayBadgeSize.SMALL -> 4.dp to 2.dp
        DelayBadgeSize.MEDIUM -> 8.dp to 4.dp
    }
    val textStyle = when (size) {
        DelayBadgeSize.SMALL -> MaterialTheme.typography.labelSmall
        DelayBadgeSize.MEDIUM -> MaterialTheme.typography.labelMedium
    }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = hPad, vertical = vPad),
            color = MaterialTheme.colorScheme.error,
            style = textStyle,
            fontWeight = FontWeight.Bold
        )
    }
}
