package com.homesoil.app.ui.screens.scripts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.homesoil.app.ui.components.CronInput
import com.homesoil.app.ui.components.validateCron
import com.homesoil.app.ui.theme.HomesoilGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    viewModel: ScriptEditorViewModel,
    onBack: () -> Unit
) {
    val title by viewModel.title.collectAsState()
    val code by viewModel.code.collectAsState()
    val schedule by viewModel.schedule.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val isValid = title.isNotBlank() && code.isNotBlank()
    val isScheduleValid = validateCron(schedule)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNewScript) "New Script" else "Edit Script") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!viewModel.isNewScript) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Title") },
                placeholder = { Text("My Script") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Code editor
            OutlinedTextField(
                value = code,
                onValueChange = viewModel::updateCode,
                label = { Text("Code") },
                placeholder = { Text("// Write your script here\ntoggle_actuator(1)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace
                ),
                minLines = 10
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Schedule section (only for existing scripts)
            if (!viewModel.isNewScript) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Schedule",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        CronInput(
                            value = schedule,
                            onValueChange = viewModel::updateSchedule,
                            onClear = { viewModel.updateSchedule("") }
                        )

                        if (!isScheduleValid && schedule.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Invalid cron expression",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = viewModel::saveSchedule,
                            enabled = isScheduleValid,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (schedule.isBlank()) "Remove Schedule" else "Save Schedule")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!viewModel.isNewScript) {
                    OutlinedButton(
                        onClick = viewModel::run,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run")
                    }
                }

                Button(
                    onClick = { viewModel.save(onBack) },
                    enabled = isValid,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HomesoilGreen
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Script") },
            text = { Text("Are you sure you want to delete this script? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(onBack)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
