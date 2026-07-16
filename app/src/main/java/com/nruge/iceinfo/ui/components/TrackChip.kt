package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Zeigt ein Gleis/Bahnsteig an. Wurde das Gleis geändert ([changed] == true),
 * wird es in einem roten Chip hervorgehoben, sonst in der übergebenen Standard-Darstellung.
 *
 * @param plain Darstellung, wenn das Gleis nicht geändert wurde (z.B. einfacher Text oder neutraler Chip).
 */
@Composable
fun TrackLabel(
    text: String,
    changed: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    fontWeight: FontWeight = FontWeight.SemiBold,
    plain: @Composable () -> Unit
) {
    if (changed) {
        TrackChangedChip(text = text, modifier = modifier, style = style, fontWeight = fontWeight)
    } else {
        plain()
    }
}

/** Roter Chip für ein geändertes Gleis. */
@Composable
fun TrackChangedChip(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = style,
            fontWeight = fontWeight,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
