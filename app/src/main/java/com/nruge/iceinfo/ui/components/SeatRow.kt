package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.Coach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatRow(
    coaches: List<Coach>,
    selectedCoach: Int?,
    seatNumber: String,
    onCoachChange: (Int?) -> Unit,
    onSeatChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var coachExpanded by remember { mutableStateOf(false) }
        AppCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.home_coach_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                ExposedDropdownMenuBox(
                    expanded = coachExpanded,
                    onExpandedChange = { coachExpanded = !coachExpanded }
                ) {
                    Surface(
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = selectedCoach?.toString() ?: "–",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = coachExpanded)
                        }
                    }
                    ExposedDropdownMenu(
                        expanded = coachExpanded,
                        onDismissRequest = { coachExpanded = false },
                        shape = MaterialTheme.shapes.large,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("–") },
                            leadingIcon = { if (selectedCoach == null) Icon(Icons.Default.Check, null) },
                            onClick = { onCoachChange(null); coachExpanded = false }
                        )
                        coaches.forEach { coach ->
                            DropdownMenuItem(
                                text = { Text(coach.coachNumber.toString()) },
                                leadingIcon = { if (selectedCoach == coach.coachNumber) Icon(Icons.Default.Check, null) },
                                onClick = { onCoachChange(coach.coachNumber); coachExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        AppCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.home_seat_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = seatNumber,
                            onValueChange = onSeatChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (seatNumber.isEmpty()) {
                                    Text(
                                        text = "–",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }
            }
        }
    }
}
