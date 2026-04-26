package com.nruge.iceinfo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R

@Composable
fun OnboardingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_welcome),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OnboardingFeatureItem(
                    icon = Icons.Default.Speed,
                    text = stringResource(R.string.onboarding_feature_live)
                )
                OnboardingFeatureItem(
                    icon = Icons.Default.Notifications,
                    text = stringResource(R.string.onboarding_feature_notif)
                )
                OnboardingFeatureItem(
                    icon = Icons.Default.Map,
                    text = stringResource(R.string.onboarding_feature_map)
                )
                OnboardingFeatureItem(
                    icon = Icons.Default.Games,
                    text = stringResource(R.string.onboarding_feature_demo)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_button))
            }
        }
    )
}

@Composable
private fun OnboardingFeatureItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
