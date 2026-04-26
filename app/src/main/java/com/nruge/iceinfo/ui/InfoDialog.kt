package com.nruge.iceinfo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nruge.iceinfo.R

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Train, contentDescription = null) },
        title = { Text(stringResource(R.string.app_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ListItem(
                    leadingContent = { Icon(Icons.Default.Info, null) },
                    headlineContent = { Text(stringResource(R.string.info_version)) }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.info_description)) }
                )
                HorizontalDivider()
                ListItem(
                    leadingContent = { Icon(Icons.Default.Lock, null) },
                    overlineContent = { Text(stringResource(R.string.info_privacy_title)) },
                    headlineContent = { Text(stringResource(R.string.info_privacy_text)) }
                )
                HorizontalDivider()
                ListItem(
                    leadingContent = { Icon(Icons.Default.Cloud, null) },
                    overlineContent = { Text(stringResource(R.string.info_api_title)) },
                    headlineContent = { Text(stringResource(R.string.info_api_url)) }
                )
                HorizontalDivider()
                ListItem(
                    leadingContent = { Icon(Icons.Default.Code, null) },
                    overlineContent = { Text(stringResource(R.string.info_built_with_title)) },
                    headlineContent = { Text(stringResource(R.string.info_built_with_text)) }
                )
                HorizontalDivider()
                ListItem(
                    leadingContent = { Icon(Icons.Default.Gavel, null) },
                    overlineContent = { Text(stringResource(R.string.info_legal_title)) },
                    headlineContent = {
                        Column {
                            Text(stringResource(R.string.info_legal_1))
                            Text(stringResource(R.string.info_legal_2))
                            Text(stringResource(R.string.info_legal_3))
                            Text(stringResource(R.string.info_legal_4))
                        }
                    }
                )
                HorizontalDivider()
                ListItem(
                    leadingContent = { Icon(Icons.Default.Favorite, null) },
                    headlineContent = { Text("Für Jan und Marek") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.info_close))
            }
        }
    )
}
