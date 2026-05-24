package com.nruge.iceinfo.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import com.nruge.iceinfo.ui.theme.LocalDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTetheringError
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.ui.theme.ICEInfoTheme

@Composable
fun NoWifiScreen(
    modifier: Modifier = Modifier,
    status: TrainStatus? = null,
    isWIFIonICE: Boolean = false,
    onRetry: () -> Unit = {},
    onMockMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 21.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        // Background photo: blur + background color tint on one layer
        Image(
            painter = painterResource(id = R.drawable.welcome_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(background.copy(alpha = 0.6f), BlendMode.SrcOver),
            modifier = Modifier
                .fillMaxSize()
                .blur(16.dp)
        )

        // Content
        val textColor = if (LocalDarkTheme.current) Color.White else Color.Black

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title + subtitle shifted upward
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.welcome_title),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Text(
                        text = stringResource(R.string.welcome_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            // Action cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WelcomeActionCard(
                    icon = if (isWIFIonICE) Icons.Default.WifiTetheringError else Icons.Default.Wifi,
                    title = stringResource(
                        if (isWIFIonICE) R.string.no_wifi_api_hint
                        else R.string.welcome_connect_title
                    ),
                    description = stringResource(
                        if (isWIFIonICE) R.string.no_wifi_text
                        else R.string.welcome_connect_desc
                    ),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                        onRetry()
                    }
                )

                WelcomeActionCard(
                    icon = Icons.Default.PlayArrow,
                    title = stringResource(R.string.demo_mode),
                    description = stringResource(R.string.welcome_demo_desc),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onMockMode
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { uriHandler.openUri("https://www.buymeacoffee.com/nicoruge") },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFDD00)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "☕", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Buy me a coffee",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nicos ICEinfo",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_bluesky),
                            contentDescription = "BlueSky",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { uriHandler.openUri("https://bsky.app/profile/nico-ruge.de") }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = "GitHub",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { uriHandler.openUri("https://github.com/nicoruge/ice-info-live") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = contentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun NoWifiScreenPreview() {
    ICEInfoTheme {
        NoWifiScreen()
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun NoWifiScreenApiDownPreview() {
    ICEInfoTheme {
        NoWifiScreen(isWIFIonICE = true)
    }
}
