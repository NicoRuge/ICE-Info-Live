package com.nruge.iceinfo.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTetheringError
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.ui.theme.ICEInfoTheme
import com.nruge.iceinfo.ui.theme.LocalDarkTheme

private val carouselScreenshotsLight = listOf(
    R.drawable.screenshot_1_light,
    R.drawable.screenshot_2_light,
    R.drawable.screenshot_3_light,
    R.drawable.screenshot_4_light,
)

private val carouselScreenshotsDark = listOf(
    R.drawable.screenshot_1_dark,
    R.drawable.screenshot_2_dark,
    R.drawable.screenshot_3_dark,
    R.drawable.screenshot_4_dark,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoWifiScreen(
    modifier: Modifier = Modifier,
    status: TrainStatus? = null,
    isWIFIonICE: Boolean = false,
    onRetry: () -> Unit = {},
    onMockMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val screenshots = if (LocalDarkTheme.current) carouselScreenshotsDark else carouselScreenshotsLight

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screenshot carousel with parallax
        val carouselState = rememberCarouselState { screenshots.size }

        HorizontalUncontainedCarousel(
            state = carouselState,
            itemWidth = 220.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .padding(top = 16.dp)
        ) { index ->
            val drawable = screenshots[index]
            Image(
                painter = painterResource(id = drawable),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(450.dp)
                    .maskClip(MaterialTheme.shapes.extraLarge)
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(
                if (isWIFIonICE) R.string.no_wifi_api_subtitle
                else R.string.welcome_subtitle
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(60.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isWIFIonICE) {
                WelcomeActionCard(
                    icon = Icons.Default.WifiTetheringError,
                    title = stringResource(R.string.no_wifi_api_title),
                    description = stringResource(R.string.no_wifi_api_desc),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = null
                )
                WelcomeActionCard(
                    icon = Icons.Default.PlayArrow,
                    title = stringResource(R.string.no_wifi_demo_title),
                    description = stringResource(R.string.welcome_demo_desc),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onMockMode
                )
            } else {
                WelcomeActionCard(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.welcome_connect_title),
                    description = stringResource(R.string.welcome_connect_desc),
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
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.buymeacoffee_button),
            contentDescription = "Buy Me a Coffee",
            modifier = Modifier
                .height(40.dp)
                .clickable {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/nicoruge")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://bsky.app/profile/nico-ruge.de")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bluesky),
                        contentDescription = "BlueSky",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                IconButton(onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/NicoRuge/ICE-Info-Live")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_github),
                        contentDescription = "GitHub",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                IconButton(onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("mailto:iceinfo@nico-ruge.de")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mail),
                        contentDescription = "E-Mail",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                IconButton(onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.nruge.iceinfo")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_googleplay),
                        contentDescription = "Google Play",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun WelcomeActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    onClick: (() -> Unit)?
) {
    val cardContent: @Composable () -> Unit = {
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
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = containerColor,
            contentColor = contentColor
        ) {
            cardContent()
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = containerColor,
            contentColor = contentColor
        ) {
            cardContent()
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun NoWifiScreenPreview() {
    ICEInfoTheme {
        NoWifiScreen()
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun NoWifiScreenApiDownPreview() {
    ICEInfoTheme {
        NoWifiScreen(isWIFIonICE = true)
    }
}
