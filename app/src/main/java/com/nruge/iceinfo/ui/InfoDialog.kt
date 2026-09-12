package com.nruge.iceinfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Train, contentDescription = null)
                Text(
                    stringResource(R.string.app_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            InfoSection {
                InfoRow(
                    icon = Icons.Default.Info,
                    headline = stringResource(R.string.info_version, com.nruge.iceinfo.BuildConfig.VERSION_NAME)
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
                ListItem(
                    leadingContent = { Icon(Icons.Default.Cloud, contentDescription = null) },
                    overlineContent = { Text(stringResource(R.string.info_api_title)) },
                    headlineContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.info_api_url1))
                            Text(stringResource(R.string.info_api_url2))
                            Text(stringResource(R.string.info_api_url3))
                            Text(stringResource(R.string.info_api_url4))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    leadingContent = { Icon(Icons.Default.Cloud, contentDescription = null) },
                    overlineContent = { Text(stringResource(R.string.info_api_title_db)) },
                    headlineContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.info_api_db_stada))
                            Text(stringResource(R.string.info_api_db_fasta))
                            Text(stringResource(R.string.info_api_db_wagenreihung))
                            Text(stringResource(R.string.info_api_db_abfahrten))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    leadingContent = { Icon(Icons.Default.Cloud, contentDescription = null) },
                    overlineContent = { Text(stringResource(R.string.info_api_title_community)) },
                    headlineContent = {
                        Text(stringResource(R.string.info_api_community_transport))
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                            Text(stringResource(R.string.info_legal_5))
                            Text(stringResource(R.string.info_legal_6))
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
    }
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
