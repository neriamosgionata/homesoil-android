package com.homesoil.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homesoil.app.data.models.Actuator
import com.homesoil.app.ui.theme.*

private val IntermittentAmber = Color(0xFFF59E0B)

@Composable
fun ActuatorCard(
    actuator: Actuator,
    onToggle: () -> Unit,
    onPulse: () -> Unit,
    onIntermittent: (onMs: Int, offMs: Int) -> Unit,
    onStopIntermittent: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(actuator.displayName) }
    var showIntermittentConfig by remember { mutableStateOf(false) }
    var onMs by remember { mutableStateOf("1000") }
    var offMs by remember { mutableStateOf("1000") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (actuator.state) OnlineGreen else DarkOnSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actuator.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    OnlineIndicator(online = actuator.online)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (actuator.pulse) {
                // Pulse actuator
                Button(
                    onClick = onPulse,
                    enabled = actuator.online,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HomesoilGreenDark,
                        disabledContainerColor = DarkSurface
                    )
                ) {
                    if (actuator.state) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pulse")
                    }
                }
            } else {
                // Toggle actuator with intermittent support
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = actuator.state,
                            onCheckedChange = { onToggle() },
                            enabled = actuator.online,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = HomesoilGreen,
                                uncheckedThumbColor = DarkOnSurfaceVariant,
                                uncheckedTrackColor = DarkSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (actuator.state) "On" else "Off",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (actuator.state) HomesoilGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Intermittent button
                    OutlinedButton(
                        onClick = {
                            if (actuator.intermittent) {
                                onStopIntermittent()
                            } else {
                                showIntermittentConfig = !showIntermittentConfig
                            }
                        },
                        enabled = actuator.online,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (actuator.intermittent) IntermittentAmber else Color.Transparent,
                            contentColor = if (actuator.intermittent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (actuator.intermittent) IntermittentAmber else MaterialTheme.colorScheme.outline
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (actuator.intermittent) "Stop" else "Intermittent",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Intermittent config panel
                if (showIntermittentConfig) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ON",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(28.dp)
                            )
                            OutlinedTextField(
                                value = onMs,
                                onValueChange = { onMs = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "OFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(28.dp)
                            )
                            OutlinedTextField(
                                value = offMs,
                                onValueChange = { offMs = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                val on = onMs.toIntOrNull() ?: 1000
                                val off = offMs.toIntOrNull() ?: 1000
                                onIntermittent(on, off)
                                showIntermittentConfig = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IntermittentAmber
                            )
                        ) {
                            Text("Start Intermittent", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Active intermittent status
                if (actuator.intermittent) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cycling: ${actuator.intermittentOnMs}ms on / ${actuator.intermittentOffMs}ms off",
                        style = MaterialTheme.typography.labelSmall,
                        color = IntermittentAmber
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Actuator") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(newName)
                        showRenameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
