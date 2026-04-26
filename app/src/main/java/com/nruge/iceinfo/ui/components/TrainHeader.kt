package com.nruge.iceinfo.ui.components

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
import androidx.compose.ui.tooling.preview.Preview
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.sampleTrainStatus
import com.nruge.iceinfo.ui.theme.DBRot
import com.nruge.iceinfo.ui.theme.ICEInfoTheme
import com.nruge.iceinfo.util.getIceClass
import com.nruge.iceinfo.util.getIceDrawable

@Composable
fun TrainHeader(status: TrainStatus) {
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableStateOf(300f) }
    var trackOffset by remember { mutableStateOf(0f) }
    val currentStatus by rememberUpdatedState(status)

    LaunchedEffect(Unit) {
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
                .offset(x = (trackOffset - 17).dp, y = (-45).dp)
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
            painter = painterResource(id = getIceDrawable(status.tzn)),
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

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp),
            colors = CardDefaults.elevatedCardColors(
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
                        color = DBRot
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
                    color = DBRot
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrainHeaderPreview() {
    ICEInfoTheme {
        TrainHeader(status = sampleTrainStatus)
    }
}
