package com.nruge.iceinfo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.sampleTrainStatus
import com.nruge.iceinfo.ui.theme.ICEInfoTheme
import com.nruge.iceinfo.ui.theme.LocalDarkTheme
import com.nruge.iceinfo.util.IceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun TrainHeader(status: TrainStatus, reducedMotion: Boolean = false) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val speedAlpha by animateFloatAsState(
        targetValue = if (copied) 0.4f else 1f,
        animationSpec = tween(150),
        finishedListener = { if (copied) copied = false },
        label = "speedCopiedAlpha"
    )
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableStateOf(300f) }
    var trackOffset by remember { mutableStateOf(0f) }
    val currentStatus by rememberUpdatedState(status)

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            trackOffset = 0f
            return@LaunchedEffect
        }
        var lastTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { time ->
                val dt = (time - lastTime) / 1_000_000_000f
                lastTime = time

                val speed = currentStatus.speed
                if (speed > 0) {
                    val trackWidthDp = trackWidthPx / density.density
                    val velocity = (trackWidthDp * speed) / 300f
                    trackOffset -= dt * velocity

                    if (trackOffset <= -trackWidthDp) {
                        trackOffset += trackWidthDp
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(5.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .graphicsLayer { clip = false }
    )
    {
        Row(
            modifier = Modifier
                .height(50.dp)
                .wrapContentWidth(unbounded = true, align = Alignment.Start)
                .align(Alignment.CenterStart)
                .offset { IntOffset((trackOffset - 17f).dp.roundToPx(), (-45).dp.roundToPx()) }
                .zIndex(1f)
                .onGloballyPositioned { coords ->
                    trackWidthPx = coords.size.width / 5f
                }
        ) {
            repeat(5) {
                Image(
                    painter = painterResource(id = R.drawable.traintracks),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .height(50.dp)
                        .wrapContentWidth(unbounded = true)
                )
            }
        }

        Image(
            painter = painterResource(id = IceUtils.getIceDrawable(status.tzn)),
            contentDescription = null,
            alignment = Alignment.CenterStart,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .height(44.dp)
                .wrapContentWidth(unbounded = true, align = Alignment.Start)
                .align(Alignment.CenterStart)
                .offset(x = (-150).dp, y = (-44).dp)
                .zIndex(2f)
                .graphicsLayer { clip = false }
        )

        val isRainbowIce = IceUtils.getSpecialName(status.tzn)
            ?.name?.contains("Regenbogen", ignoreCase = true) == true

        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isRainbowIce) Modifier.drawBehind { drawRainbowStripes() } else Modifier)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${status.trainType} ${status.trainNumber}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    val seriesLabel = IceUtils.getIceClassFromSeries(status.series, status.tzn)
                    val vmax = IceUtils.getIceVmax(status.series, status.tzn)
                    val specialName = IceUtils.getSpecialName(status.tzn)
                    val tzName = IceUtils.getTzName(status.tzn)
                    val baseLabel = listOfNotNull(
                        seriesLabel.ifEmpty { null },
                        tzName?.let { "„${it.name}“" }
                    ).joinToString(" ")
                    val hasDetails = vmax != null || specialName != null
                    var expanded by remember { mutableStateOf(false) }

                    if (baseLabel.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(enabled = hasDetails) { expanded = !expanded }
                        ) {
                            Text(
                                text = baseLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            if (hasDetails) {
                                Text(
                                    text = "›",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .padding(start = 3.dp)
                                        .rotate(if (expanded) 180f else 0f)
                                )
                            }
                            AnimatedVisibility(
                                visible = expanded,
                                enter = expandHorizontally() + fadeIn(),
                                exit = shrinkHorizontally() + fadeOut()
                            ) {
                                val details = buildList {
                                    if (specialName != null) add(specialName.name)
                                    if (vmax != null) add("Vmax $vmax km/h")
                                }.joinToString(" · ")
                                Text(
                                    text = "  $details",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
                    val isDark = LocalDarkTheme.current
                    Text(
                        text = "${status.speed} km/h",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .graphicsLayer { alpha = speedAlpha }
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        copySpeedCardToClipboard(context, status, isDark)
                                    }
                                    copied = true
                                }
                            }
                    )
            }
        }
    }
}

/** Schräge Pride-Streifen analog zur Sonderlackierung des Regenbogen-ICE (Tz 304). */
private fun DrawScope.drawRainbowStripes() {
    val colors = listOf(
        Color(0xFFE40303), // Rot
        Color(0xFFFF8C00), // Orange
        Color(0xFFFFED00), // Gelb
        Color(0xFF008026), // Grün
        Color(0xFF004DFF), // Blau
        Color(0xFF750787)  // Violett
    )
    val stroke = 20.dp.toPx()
    val slant = size.height * 0.4f
    // Horizontaler Versatz, bei dem sich die schrägen Linien gerade berühren
    val lineHeight = size.height + 2 * stroke
    val dx = stroke * kotlin.math.hypot(lineHeight, slant) / lineHeight
    colors.forEachIndexed { i, color ->
        val x = size.width * 0.01f + i * dx
        drawLine(
            color = color,
            start = Offset(x + slant, -stroke),
            end = Offset(x, size.height + stroke),
            strokeWidth = stroke,
            alpha = 0.35f
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TrainHeaderPreview() {
    ICEInfoTheme {
        TrainHeader(status = sampleTrainStatus)
    }
}
