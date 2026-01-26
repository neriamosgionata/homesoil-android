package com.homesoil.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CronInput(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Cron Schedule") },
            placeholder = { Text("* * * * *") },
            enabled = enabled,
            singleLine = true,
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Help text for cron format
        Text(
            text = "Format: minute hour day month weekday",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Quick presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CronPresetChip(
                label = "Every minute",
                cronValue = "* * * * *",
                onClick = { onValueChange("* * * * *") },
                enabled = enabled
            )
            CronPresetChip(
                label = "Hourly",
                cronValue = "0 * * * *",
                onClick = { onValueChange("0 * * * *") },
                enabled = enabled
            )
            CronPresetChip(
                label = "Daily",
                cronValue = "0 0 * * *",
                onClick = { onValueChange("0 0 * * *") },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun CronPresetChip(
    label: String,
    cronValue: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        enabled = enabled,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    )
}

fun validateCron(cron: String): Boolean {
    if (cron.isBlank()) return true // Empty is valid (no schedule)

    val parts = cron.trim().split("\\s+".toRegex())
    if (parts.size != 5) return false

    val patterns = listOf(
        // Minute: 0-59
        "^(\\*|([0-5]?\\d)(,([0-5]?\\d))*|([0-5]?\\d)-([0-5]?\\d)|\\*/([1-5]?\\d))$",
        // Hour: 0-23
        "^(\\*|([01]?\\d|2[0-3])(,([01]?\\d|2[0-3]))*|([01]?\\d|2[0-3])-([01]?\\d|2[0-3])|\\*/([01]?\\d|2[0-3]))$",
        // Day: 1-31
        "^(\\*|([1-9]|[12]\\d|3[01])(,([1-9]|[12]\\d|3[01]))*|([1-9]|[12]\\d|3[01])-([1-9]|[12]\\d|3[01])|\\*/([1-9]|[12]\\d|3[01]))$",
        // Month: 1-12
        "^(\\*|([1-9]|1[0-2])(,([1-9]|1[0-2]))*|([1-9]|1[0-2])-([1-9]|1[0-2])|\\*/([1-9]|1[0-2]))$",
        // Weekday: 0-6
        "^(\\*|[0-6](,[0-6])*|[0-6]-[0-6]|\\*/[0-6])$"
    )

    return parts.zip(patterns).all { (part, pattern) ->
        part.matches(Regex(pattern))
    }
}
