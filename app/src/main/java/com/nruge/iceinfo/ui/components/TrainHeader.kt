package com.nruge.iceinfo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.util.getIceClass
import com.nruge.iceinfo.util.getIceDrawable

@Composable
fun TrainHeader(status: TrainStatus) {
    val density = LocalDensity.current
    var trackWidth by remember { mutableStateOf(300f) }

    val speedBucket = (status.speed / 10) * 10

    val animDuration = if (status.speed > 0) {
        (300000 / status.speed.coerceAtLeast(1)).coerceIn(500, 8000)
    } else {
        8000
    }

    key(speedBucket) {
        val infiniteTransition = rememberInfiniteTransition(label = "tracks")

        val targetValue = if (status.speed > 0) -trackWidth / density.density else 0f

        val trackOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(animDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "trackScroll"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .graphicsLayer { clip = false }
        ) {
            Row(
                modifier = Modifier
                    .height(70.dp)
                    .wrapContentWidth(unbounded = true, align = Alignment.Start)
                    .align(Alignment.CenterStart)
                    .offset(x = (trackOffset - 17).dp, y = (-55).dp)
                    .zIndex(1f)
                    .onGloballyPositioned { coords ->
                        trackWidth = coords.size.width / 5f
                    }
            ) {
                repeat(5) {
                    Image(
                        painter = painterResource(id = R.drawable.traintracks),
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier
                            .height(70.dp)
                            .wrapContentWidth(unbounded = true)
                    )
                }
            }

            Image(
                painter = painterResource(id = getIceDrawable(status.tzn)),
                contentDescription = null,
                alignment = Alignment.CenterStart,
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .height(63.dp)
                    .wrapContentWidth(unbounded = true, align = Alignment.Start)
                    .align(Alignment.CenterStart)
                    .offset(x = (-250).dp, y = (-54).dp)
                    .zIndex(2f)
                    .graphicsLayer { clip = false }
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = getIceClass(status.tzn),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "${status.speed} km/h",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
