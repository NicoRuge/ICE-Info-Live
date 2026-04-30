package com.nruge.iceinfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Train, contentDescription = null) },
        title = {
            Text(
                stringResource(R.string.app_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoSection {
                    InfoRow(
                        icon = Icons.Default.Info,
                        headline = stringResource(R.string.info_version)
                    )
                    InfoRow(
                        headline = stringResource(R.string.info_description)
                    )
                }

                InfoSection {
                    InfoRow(
                        icon = Icons.Default.Lock,
                        overline = stringResource(R.string.info_privacy_title),
                        headline = stringResource(R.string.info_privacy_text)
                    )
                    InfoRow(
                        icon = Icons.Default.Cloud,
                        overline = stringResource(R.string.info_api_title),
                        headline = stringResource(R.string.info_api_url)
                    )
                    InfoRow(
                        icon = Icons.Default.Code,
                        overline = stringResource(R.string.info_built_with_title),
                        headline = stringResource(R.string.info_built_with_text)
                    )
                }

                InfoSection {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Gavel, null) },
                        overlineContent = { Text(stringResource(R.string.info_legal_title)) },
                        headlineContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(stringResource(R.string.info_legal_1))
                                Text(stringResource(R.string.info_legal_2))
                                Text(stringResource(R.string.info_legal_3))
                                Text(stringResource(R.string.info_legal_4))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }

                InfoSection {
                    InfoRow(
                        icon = Icons.Default.Favorite,
                        headline = "Für Jan und Marek"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.info_close))
            }
        }
    )
}

@Composable
private fun InfoSection(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun InfoRow(
    headline: String,
    icon: ImageVector? = null,
    overline: String? = null
) {
    ListItem(
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        overlineContent = overline?.let { { Text(it) } },
        headlineContent = { Text(headline) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
