package com.nruge.iceinfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Train, contentDescription = null) },
        title = { Text(stringResource(R.string.app_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.info_version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.info_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.info_privacy_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(stringResource(R.string.info_privacy_text))
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.info_api_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(R.string.info_api_url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.info_built_with_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(R.string.info_built_with_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.info_legal_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(stringResource(R.string.info_legal_1))
                Text(stringResource(R.string.info_legal_2))
                Text(stringResource(R.string.info_legal_3))
                Text(stringResource(R.string.info_legal_4))
                HorizontalDivider()
                Text("Für Jan und Marek")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.info_close))
            }
        }
    )
}
