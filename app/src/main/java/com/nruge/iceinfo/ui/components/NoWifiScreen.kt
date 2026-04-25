package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.ui.theme.ICEInfoTheme
import com.nruge.iceinfo.util.getIceDrawable

@Composable
fun NoWifiScreen(
    modifier: Modifier = Modifier,
    status: TrainStatus? = null,
    onRetry: () -> Unit = {},
    onMockMode: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .graphicsLayer { clip = false }
            ) {
                Row(
                    modifier = Modifier
                        .height(80.dp)
                        .wrapContentWidth(unbounded = true, align = Alignment.Start)
                        .align(Alignment.CenterStart)
                        .offset(x = (-50).dp, y = (-55).dp)
                        .zIndex(1f)
                ) {
                    repeat(2) {
                        Image(
                            painter = painterResource(id = R.drawable.traintracks),
                            contentDescription = null,
                            contentScale = ContentScale.FillHeight,
                            modifier = Modifier
                                .height(100.dp)
                                .wrapContentWidth(unbounded = true)
                        )
                    }
                }
                Image(
                    painter = painterResource(id = getIceDrawable(status?.tzn ?: "")),
                    contentDescription = null,
                    alignment = Alignment.CenterStart,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .height(72.dp)
                        .wrapContentWidth(unbounded = true, align = Alignment.Start)
                        .align(Alignment.CenterStart)
                        .offset(x = (-350).dp, y = (-54).dp)
                        .zIndex(2f)
                        .graphicsLayer { clip = false }
                )
            }
            Text(
                text = stringResource(R.string.no_wifi_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.no_wifi_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.no_wifi_connect))
            }
            Spacer(modifier = Modifier.height(50.dp))
            Text(
                text = stringResource(R.string.demo_mode_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            FilledTonalButton(
                onClick = onMockMode,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Text(
                    text = stringResource(R.string.demo_mode),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NoWifiScreenPreview() {
    ICEInfoTheme {
        NoWifiScreen()
    }
}
