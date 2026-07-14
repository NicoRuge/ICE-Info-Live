package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.Coach
import com.nruge.iceinfo.sampleCoaches
import com.nruge.iceinfo.ui.theme.ICEInfoTheme

private val COACH_HEIGHT = 56.dp
private val NOSE_DEPTH   = 18.dp

// ─── Public card ─────────────────────────────────────────────────────────────

@Composable
fun WagenreihungCard(coaches: List<Coach>, stopName: String = "", selectedCoach: Int?) {
    if (coaches.isEmpty()) return

    val firstClassBg  = MaterialTheme.colorScheme.primaryContainer
    val secondClassBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val diningBg      = MaterialTheme.colorScheme.tertiaryContainer
    val locoBg        = MaterialTheme.colorScheme.secondaryContainer
    val selectedColor = MaterialTheme.colorScheme.primary
    val outlineColor  = MaterialTheme.colorScheme.outlineVariant
    val onSurface     = MaterialTheme.colorScheme.onSurface
    val onSurfaceVar  = MaterialTheme.colorScheme.onSurfaceVariant

    // Group consecutive same-sector coaches into segments (preserves train order)
    data class Segment(val sector: String, val coaches: List<Coach>)
    val segments = coaches.fold(listOf<Segment>()) { acc, coach ->
        val last = acc.lastOrNull()
        if (last != null && last.sector == coach.sector) {
            acc.dropLast(1) + last.copy(coaches = last.coaches + coach)
        } else {
            acc + Segment(coach.sector, listOf(coach))
        }
    }

    val hasDining = coaches.any { it.vehicleCategory.contains("DINING", ignoreCase = true) }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Header: title + class legend ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = stringResource(R.string.home_coach_map_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (stopName.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.home_coach_map_subtitle, stopName),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WagonLegendChip(firstClassBg,  outlineColor, "1. Kl.", onSurfaceVar)
                    WagonLegendChip(secondClassBg, outlineColor, "2. Kl.", onSurfaceVar)
                    if (hasDining) {
                        WagonLegendChip(diningBg, outlineColor, "Speise", onSurfaceVar)
                    }
                }
            }

            // ── Horizontal, scrollable train ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                segments.forEachIndexed { segIdx, segment ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        // Sector chip above the group
                        if (segment.sector.isNotBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = segment.sector,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Spacer(Modifier.height(20.dp))
                        }

                        // Wagons in this sector
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            segment.coaches.forEachIndexed { coachIdx, coach ->
                                val flatIdx = segments.take(segIdx).sumOf { it.coaches.size } + coachIdx
                                val isFirst = flatIdx == 0
                                val isLast  = flatIdx == coaches.lastIndex
                                val prevCoach = coaches.getOrNull(flatIdx - 1)
                                val prevIsLocoOrControl = prevCoach != null &&
                                    (prevCoach.vehicleCategory == "LOCOMOTIVE" ||
                                     prevCoach.vehicleCategory.contains("CONTROLCAR", ignoreCase = true))
                                val isLocoOrControl = coach.vehicleCategory == "LOCOMOTIVE" ||
                                    coach.vehicleCategory.contains("CONTROLCAR", ignoreCase = true)
                                val forceNoseRight = isLocoOrControl && !isFirst && !isLast && !prevIsLocoOrControl
                                WagonItem(
                                    coach          = coach,
                                    isSelected     = coach.coachNumber == selectedCoach,
                                    isFirstInTrain = isFirst,
                                    isLastInTrain  = isLast,
                                    forceNoseRight = forceNoseRight,
                                    firstClassBg  = firstClassBg,
                                    secondClassBg = secondClassBg,
                                    diningBg      = diningBg,
                                    locoBg        = locoBg,
                                    selectedColor = selectedColor,
                                    outlineColor  = outlineColor,
                                    onSurface     = onSurface,
                                    onSurfaceVar  = onSurfaceVar
                                )
                            }
                        }
                    }

                    // Vertical divider between sectors
                    if (segIdx < segments.lastIndex) {
                        Spacer(Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(COACH_HEIGHT + 26.dp)
                                .padding(top = 24.dp)
                                .background(outlineColor.copy(alpha = 0.35f))
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                }
            }
        }
    }
}

// ─── Single wagon ─────────────────────────────────────────────────────────────

@Composable
private fun WagonItem(
    coach: Coach,
    isSelected: Boolean,
    isFirstInTrain: Boolean,
    isLastInTrain: Boolean,
    forceNoseRight: Boolean = false,
    firstClassBg: Color,
    secondClassBg: Color,
    diningBg: Color,
    locoBg: Color,
    selectedColor: Color,
    outlineColor: Color,
    onSurface: Color,
    onSurfaceVar: Color
) {
    val cat       = coach.vehicleCategory
    val isClosed  = coach.isClosed
    val isLoco    = cat == "LOCOMOTIVE"
    val isDining  = cat.contains("DINING",      ignoreCase = true)
    val isControl = cat.contains("CONTROLCAR",  ignoreCase = true)
    val isFirst   = coach.hasFirstClass && !coach.hasSecondClass
    val isMixed   = coach.hasFirstClass && coach.hasSecondClass

    val closedBg     = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
    val closedBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val bgColor = when {
        isClosed  -> closedBg
        isLoco    -> locoBg
        isDining  -> diningBg
        isFirst || isMixed -> firstClassBg
        else      -> secondClassBg
    }
    val borderColor = when {
        isSelected -> selectedColor
        isClosed   -> closedBorder
        else       -> outlineColor
    }
    val borderDp    = if (isSelected) 2.dp else 1.dp
    val winAlpha    = when {
        isClosed   -> 0f      // gesperrt: keine Fenster zeichnen
        isSelected -> 0.28f
        else       -> 0.15f
    }

    val coachWidth = when {
        isLoco   -> 36.dp
        isDining -> 56.dp
        else     -> 46.dp
    }

    // For control cars: nose on the outward end of the train.
    // forceNoseRight handles middle locos in Doppeltraktion (rear of first train section).
    val noseLeft  = isFirstInTrain
    val noseRight = isLastInTrain || forceNoseRight

    val diningIconTint = MaterialTheme.colorScheme.onTertiaryContainer

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // ── Wagon body: Canvas + optional icon overlay ────────────────────────
        val bdp  = borderDp
        val fcBg = firstClassBg
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = coachWidth, height = COACH_HEIGHT)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sw      = bdp.toPx()
                val r       = 4.dp.toPx()
                val nose    = NOSE_DEPTH.toPx()
                val numCols = if (coachWidth == 46.dp) 3 else 2

                when {
                    isControl || isLoco -> {
                        val path = if (noseRight) buildNoseRight(size, r, nose)
                                   else           buildNoseLeft(size, r, nose)
                        drawPath(path, bgColor)
                        if (isMixed) {
                            withTransform({ clipRect(size.width / 2, 0f, size.width, size.height) }) {
                                drawPath(path, fcBg)
                            }
                        }
                        drawPath(path, borderColor, style = Stroke(width = sw))
                        // Windows shifted away from the nose
                        val winStart = if (!noseRight) nose + 3.dp.toPx() else 3.dp.toPx()
                        val winEnd   = if (noseRight)  size.width - nose - 3.dp.toPx() else size.width - 3.dp.toPx()
                        drawWagonWindows(winStart, winEnd, numCols = 2, onSurface, winAlpha)
                    }

                    isDining -> {
                        drawRoundRect(bgColor, cornerRadius = CornerRadius(r))
                        drawRoundRect(borderColor, cornerRadius = CornerRadius(r), style = Stroke(sw))
                        // Icon is overlaid as a Composable below
                    }

                    else -> {
                        drawRoundRect(bgColor, cornerRadius = CornerRadius(r))
                        if (isMixed) {
                            withTransform({ clipRect(0f, 0f, size.width / 2, size.height) }) {
                                drawRoundRect(fcBg, cornerRadius = CornerRadius(r))
                            }
                        }
                        drawRoundRect(borderColor, cornerRadius = CornerRadius(r), style = Stroke(sw))
                        drawWagonWindows(3.dp.toPx(), size.width - 3.dp.toPx(), numCols, onSurface, winAlpha)
                    }
                }
            }

            // Material Icon overlay for dining car
            if (isDining && !isClosed) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = diningIconTint
                )
            }

            // Lock overlay for closed/locked wagons
            if (isClosed) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.coach_closed),
                    modifier = Modifier.size(18.dp),
                    tint = onSurfaceVar
                )
            }
        }

        // ── Coach number ──────────────────────────────────────────────────────
        Text(
            text = when {
                isClosed -> stringResource(R.string.coach_closed)
                isLoco   -> ""
                else     -> coach.coachNumber.toString()
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> selectedColor
                isClosed   -> onSurfaceVar
                else       -> onSurface
            }
        )

        // ── Amenity icons (Material Icons, not emojis) ────────────────────────
        val icons = coach.amenities.mapNotNull { it.toWagonAmenityIcon() }
        if (icons.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                icons.take(4).forEach { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = onSurfaceVar
                    )
                }
            }
        }
    }
}

// ─── Canvas path builders ─────────────────────────────────────────────────────

/** Steuerwagen with nose pointing LEFT (= front/first wagon in display order) */
private fun buildNoseLeft(size: Size, r: Float, noseDepth: Float): Path = Path().apply {
    moveTo(noseDepth, 0f)
    lineTo(size.width - r, 0f)
    arcTo(Rect(size.width - 2 * r, 0f, size.width, 2 * r), -90f, 90f, false)
    lineTo(size.width, size.height - r)
    arcTo(Rect(size.width - 2 * r, size.height - 2 * r, size.width, size.height), 0f, 90f, false)
    lineTo(noseDepth, size.height)
    cubicTo(noseDepth * 0.4f, size.height, 0f, size.height * 0.7f, 0f, size.height / 2)
    cubicTo(0f, size.height * 0.3f, noseDepth * 0.4f, 0f, noseDepth, 0f)
    close()
}

/** Steuerwagen with nose pointing RIGHT (= rear/last wagon in display order) */
private fun buildNoseRight(size: Size, r: Float, noseDepth: Float): Path = Path().apply {
    val noseX = size.width - noseDepth
    // moveTo must match the arc's starting point (180° of top-left oval = (0, r))
    moveTo(0f, r)
    arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)   // top-left corner → (r, 0)
    lineTo(noseX, 0f)
    cubicTo(noseX + noseDepth * 0.55f, 0f, size.width, size.height * 0.3f, size.width, size.height / 2)
    cubicTo(size.width, size.height * 0.7f, noseX + noseDepth * 0.55f, size.height, noseX, size.height)
    lineTo(r, size.height)
    arcTo(Rect(0f, size.height - 2 * r, 2 * r, size.height), 90f, 90f, false)  // bottom-left corner → (0, h-r)
    close()  // line back to (0, r)
}

// ─── Canvas draw helpers ──────────────────────────────────────────────────────

/** Two rows of rounded-rectangle windows, centred within the given X range */
private fun DrawScope.drawWagonWindows(
    startX: Float,
    endX: Float,
    numCols: Int,
    onSurface: Color,
    alpha: Float
) {
    val winW  = 7.dp.toPx()
    val winH  = 9.dp.toPx()
    val gapX  = 4.dp.toPx()
    val gapY  = 5.dp.toPx()
    val rows  = 2
    val totalW = numCols * winW + (numCols - 1) * gapX
    val totalH = rows * winH + (rows - 1) * gapY
    val availW = endX - startX
    if (availW < totalW) return   // not enough room (e.g. very narrow loco)
    val sx = startX + (availW - totalW) / 2f
    val sy = (size.height - totalH) / 2f
    val color = onSurface.copy(alpha = alpha)
    repeat(rows) { row ->
        repeat(numCols) { col ->
            drawRoundRect(
                color     = color,
                topLeft   = Offset(sx + col * (winW + gapX), sy + row * (winH + gapY)),
                size      = Size(winW, winH),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}

// ─── Legend chip ──────────────────────────────────────────────────────────────

@Composable
private fun WagonLegendChip(color: Color, outline: Color, label: String, labelColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .border(1.dp, outline, RoundedCornerShape(2.dp))
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = labelColor)
    }
}

// ─── Amenity icon mapping ─────────────────────────────────────────────────────

fun String.toWagonAmenityIcon(): ImageVector? = when (this) {
    "ZONE_QUIET"             -> Icons.AutoMirrored.Filled.VolumeOff
    "ZONE_FAMILY"            -> Icons.Default.FamilyRestroom
    "ZONE_PHONE"             -> Icons.Default.PhoneEnabled
    "BIKE_SPACE"             -> Icons.AutoMirrored.Filled.DirectionsBike
    "CABIN_INFANT"           -> Icons.Default.ChildFriendly
    "WHEELCHAIR_SPACE"       -> Icons.AutoMirrored.Filled.Accessible
    "SEATS_BAHN_COMFORT"     -> Icons.Default.Star
    "SEATS_SEVERELY_DISABLED"-> Icons.AutoMirrored.Filled.Accessible
    else                     -> null
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun WagenreihungCardPreview() {
    ICEInfoTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            WagenreihungCard(coaches = sampleCoaches, stopName = "Erfurt Hbf", selectedCoach = 24)
        }
    }
}
