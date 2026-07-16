package com.nruge.iceinfo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.ui.theme.onSuccessContainer
import com.nruge.iceinfo.util.formatRemainingTimeUntil
import java.time.LocalTime

/**
 * Ankunftszeit, die zwischen Uhrzeit und verbleibender Zeit ("in Xh XXmin") überblendet —
 * gleiche Optik wie die Zeitspalte auf der Strecke-Seite (StopTimePair).
 */
@Composable
fun ArrivalTimeToggle(
    scheduled: String,
    actual: String,
    delay: Int,
    showRelative: Boolean = false,
    referenceTime: LocalTime = LocalTime.now(),
    style: TextStyle = MaterialTheme.typography.titleLarge,
    scheduledStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight = FontWeight.Black
) {
    val isDelayed = delay > 0
    val isEarly = delay < 0
    val delayColor = when {
        isEarly -> MaterialTheme.colorScheme.tertiary
        delay >= 5 -> MaterialTheme.colorScheme.error
        isDelayed -> onSuccessContainer()
        else -> MaterialTheme.colorScheme.tertiary
    }
    val timeColor = if (isDelayed || isEarly) delayColor else MaterialTheme.colorScheme.tertiary
    val displayActual = actual.ifEmpty { scheduled }
    val relativeText = if (showRelative) {
        val remaining = formatRemainingTimeUntil(scheduled, delay, referenceTime)
        if (remaining != "--") "in $remaining" else null
    } else null

    val relAlpha by animateFloatAsState(
        targetValue = if (relativeText != null) 1f else 0f,
        animationSpec = tween(350),
        label = "arrival_rel"
    )

    // Keep the last non-null relativeText so the fade-out still renders the old string
    var lastRelativeText by remember { mutableStateOf(relativeText) }
    if (relativeText != null) lastRelativeText = relativeText

    // Box keeps both states always laid out — only alpha changes, layout never moves
    Box(contentAlignment = Alignment.CenterEnd) {
        Row(
            modifier = Modifier.alpha(1f - relAlpha),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if ((isDelayed || isEarly) && scheduled.isNotEmpty()) {
                Text(
                    text = scheduled,
                    style = scheduledStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textDecoration = TextDecoration.LineThrough
                )
            }
            Text(
                text = displayActual,
                style = style,
                fontWeight = fontWeight,
                color = timeColor
            )
        }
        Text(
            text = lastRelativeText ?: "",
            modifier = Modifier.alpha(relAlpha),
            style = style,
            fontWeight = fontWeight,
            maxLines = 1,
            color = timeColor
        )
    }
}
