package com.nruge.iceinfo.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.sampleTrainStatus
import com.nruge.iceinfo.ui.theme.ICEInfoTheme
import com.nruge.iceinfo.ui.theme.LocalDarkTheme
import java.io.File

private val IceRed            = Color(0xFFCC0000)
private val IceRedLight       = Color(0xFFFF5252)
private val LiveGreen         = Color(0xFF2ECC71)

// ── Compose preview composable ────────────────────────────────────────────────

@Composable
fun SpeedShareCard(
    status: TrainStatus,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val bgStart = if (isDark) Color(0xFF0D1117) else Color(0xFFF0F2F5)
    val bgEnd = if (isDark) Color(0xFF161B22) else Color(0xFFFFFFFF)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val brandingRed = if (isDark) IceRedLight else IceRed

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(bgStart, bgEnd)
                )
            )
    ) {
        // Train tracks strip at bottom
        Row(
            modifier = Modifier
                .height(80.dp)
                .wrapContentWidth(unbounded = true)
                .align(Alignment.CenterEnd)
                .offset(x = (-130).dp, y = (78).dp)
        ) {
            repeat(2) {
                Image(
                    painter = painterResource(R.drawable.traintracks),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.height(100.dp),
                    alpha = if (isDark) 0.18f else 0.08f
                )
            }
        }

        // Train image – right side, faded
        Image(
            painter = painterResource(R.drawable.ice),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .height(80.dp)
                .wrapContentWidth(unbounded = true)
                .align(Alignment.CenterEnd)
                .offset(x = (-250).dp, y = (78).dp),
            alpha = if (isDark) 0.18f else 0.12f
        )

        // Content – left side
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Live indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(LiveGreen)
                )
                Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = LiveGreen,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            // Train number
            Text(
                text = "${status.trainType} ${status.trainNumber}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = brandingRed
            )

            // Speed – big
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${status.speed}",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary,
                    lineHeight = 72.sp
                )
                Text(
                    text = "km/h",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = textMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // Watermark
        Text(
            text = "ICEinfo",
            style = MaterialTheme.typography.labelSmall,
            color = textMuted.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 8.dp)
        )
    }
}

private fun drawTrackLines(scope: DrawScope) {
    val railColor = androidx.compose.ui.graphics.Color(0xFF30363D)
    val tieColor  = androidx.compose.ui.graphics.Color(0xFF21262D)
    val w = scope.size.width
    val h = scope.size.height

    // Rail lines
    scope.drawLine(railColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 3f)
    scope.drawLine(railColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = 3f)

    // Ties (sleepers)
    val tieSpacing = 28f
    var x = 0f
    while (x < w) {
        scope.drawRect(tieColor, topLeft = Offset(x, 0f), size = Size(14f, h))
        x += tieSpacing
    }
}

// ── Bitmap generator ──────────────────────────────────────────────────────────

fun generateSpeedShareBitmap(context: Context, status: TrainStatus, isDark: Boolean): Bitmap {
    val W = 1200
    val H = 630

    val bgStart = if (isDark) 0xFF0D1117.toInt() else 0xFFF0F2F5.toInt()
    val bgEnd = if (isDark) 0xFF1F2937.toInt() else 0xFFFFFFFF.toInt()
    val brandingRed = if (isDark) 0xFFFF5252.toInt() else 0xFFCC0000.toInt()
    val textPrimary = if (isDark) 0xFFEEEEEE.toInt() else 0xFF1A1A1A.toInt()
    val textMuted = if (isDark) 0xFF8B949E.toInt() else 0xFF65676B.toInt()
    val railColor = if (isDark) 0xFF30363D.toInt() else 0xFFD1D5DB.toInt()
    val tieColor = if (isDark) 0xFF21262D.toInt() else 0xFFE5E7EB.toInt()

    val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background gradient
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    bgPaint.shader = LinearGradient(
        0f, 0f, W.toFloat(), H.toFloat(),
        intArrayOf(bgStart, bgEnd),
        null, Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bgPaint)

    // Track strip at bottom
    drawTracksOnCanvas(canvas, W, H, railColor, tieColor)

    // Train image – right side, alpha-faded
    val trainDrawable = context.getDrawable(R.drawable.ice)
    if (trainDrawable != null) {
        val trainH = (H * 0.55).toInt()
        val trainW = (trainH * (1125f / 120f)).toInt()
        val trainBmp = Bitmap.createBitmap(trainW, trainH, Bitmap.Config.ARGB_8888)
        trainDrawable.setBounds(0, 0, trainW, trainH)
        trainDrawable.draw(Canvas(trainBmp))
        val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = if (isDark) 35 else 22 }
        canvas.drawBitmap(trainBmp, (W - trainW + 80).toFloat(), ((H - trainH) / 2 - 30).toFloat(), fadePaint)
        trainBmp.recycle()
    }

    val leftPad = 80f

    // Live indicator
    val dotRadius = 14f
    val dotY = H * 0.25f
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2ECC71.toInt() }
    canvas.drawCircle(leftPad + dotRadius, dotY, dotRadius, dotPaint)

    val livePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2ECC71.toInt()
        textSize = 32f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        letterSpacing = 0.15f
    }
    canvas.drawText("LIVE", leftPad + dotRadius * 2 + 12f, dotY + livePaint.textSize * 0.36f, livePaint)

    // Train number
    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brandingRed
        textSize = 72f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
    }
    val trainLabel = "${status.trainType} ${status.trainNumber}"
    canvas.drawText(trainLabel, leftPad, H * 0.45f, numPaint)

    // Speed – huge
    val speedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textPrimary
        textSize = 240f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val speedStr = "${status.speed}"
    canvas.drawText(speedStr, leftPad - 8f, H * 0.87f, speedPaint)

    // "km/h" unit
    val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textMuted
        textSize = 52f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val speedWidth = speedPaint.measureText(speedStr)
    canvas.drawText("km/h", leftPad + speedWidth + 16f, H * 0.85f, unitPaint)

    // Watermark
    val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = (0x44000000 or (textMuted and 0x00FFFFFF)).toInt()
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("ICEinfo", W - 32f, H - 24f, wmPaint)

    // Rounded corners via xfermode clip
    val rounded = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val roundedCanvas = Canvas(rounded)
    val roundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    roundedCanvas.drawRoundRect(RectF(0f, 0f, W.toFloat(), H.toFloat()), 48f, 48f, roundPaint)
    roundPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    roundedCanvas.drawBitmap(bitmap, 0f, 0f, roundPaint)
    bitmap.recycle()

    return rounded
}

private fun drawTracksOnCanvas(canvas: Canvas, W: Int, H: Int, railColor: Int, tieColor: Int) {
    val trackH = H * 0.12f
    val trackTop = H - trackH

    val tiePaint = Paint().apply { color = tieColor }
    val railPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = railColor
        strokeWidth = 6f
    }

    var x = 0f
    while (x < W) {
        canvas.drawRect(x, trackTop, x + 20f, H.toFloat(), tiePaint)
        x += 44f
    }
    val rail1Y = trackTop + trackH * 0.25f
    val rail2Y = trackTop + trackH * 0.75f
    canvas.drawLine(0f, rail1Y, W.toFloat(), rail1Y, railPaint)
    canvas.drawLine(0f, rail2Y, W.toFloat(), rail2Y, railPaint)
}

fun copySpeedCardToClipboard(context: Context, status: TrainStatus, isDark: Boolean) {
    val bitmap = generateSpeedShareBitmap(context, status, isDark)
    val dir  = File(context.cacheDir, "share").also { it.mkdirs() }
    val file = File(dir, "speed_card.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val clip = ClipData.newUri(context.contentResolver, "Speed Card", uri)
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
}

// ── Android Studio Preview ────────────────────────────────────────────────────

@Preview(widthDp = 480, heightDp = 252, name = "Speed Share Card – light")
@Composable
private fun SpeedShareCardPreviewLight() {
    ICEInfoTheme(darkTheme = false) {
        SpeedShareCard(
            status = sampleTrainStatus.copy(speed = 299, trainType = "ICE", trainNumber = "100"),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(widthDp = 480, heightDp = 252, name = "Speed Share Card – dark")
@Composable
private fun SpeedShareCardPreviewDark() {
    ICEInfoTheme(darkTheme = true) {
        SpeedShareCard(
            status = sampleTrainStatus.copy(speed = 299, trainType = "ICE", trainNumber = "100"),
            modifier = Modifier.fillMaxSize()
        )
    }
}
