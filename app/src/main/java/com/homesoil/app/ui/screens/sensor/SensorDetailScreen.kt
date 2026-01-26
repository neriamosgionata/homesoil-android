package com.homesoil.app.ui.screens.sensor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homesoil.app.ui.components.OnlineIndicator
import com.homesoil.app.ui.components.getSensorColor
import com.homesoil.app.ui.components.getSensorIcon
import com.homesoil.app.ui.theme.HomesoilGreen
import com.homesoil.app.util.DateUtils
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDetailScreen(
    viewModel: SensorDetailViewModel,
    onBack: () -> Unit
) {
    val sensor by viewModel.sensor.collectAsState()
    val lastRead by viewModel.lastRead.collectAsState()
    val readings by viewModel.readings.collectAsState()
    val fromDate by viewModel.fromDate.collectAsState()
    val toDate by viewModel.toDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val editingName by viewModel.editingName.collectAsState()
    val newName by viewModel.newName.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(readings) {
        if (readings.isNotEmpty()) {
            withContext(Dispatchers.Default) {
                val values = readings.mapNotNull { it.valueAsDouble }
                if (values.isNotEmpty()) {
                    modelProducer.runTransaction {
                        lineSeries { series(values) }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (editingName) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = viewModel::updateNewName,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(sensor?.displayName ?: "Sensor")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (editingName) {
                        IconButton(onClick = viewModel::saveName) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                        IconButton(onClick = viewModel::cancelEditingName) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    } else {
                        IconButton(onClick = viewModel::startEditingName) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit name")
                        }
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
        ) {
            sensor?.let { s ->
                // Sensor info card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getSensorIcon(s.sensorType),
                                    contentDescription = null,
                                    tint = getSensorColor(s.sensorType),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = s.sensorType.displayName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${s.ipAddress}:${s.port}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OnlineIndicator(online = s.online)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Current value
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = lastRead?.sensorValue ?: "--",
                                style = MaterialTheme.typography.displayMedium,
                                color = HomesoilGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.sensorType.unit,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (lastRead != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Last updated: ${DateUtils.formatDisplayDateTime(lastRead!!.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                // Date range picker
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Historical Data",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showFromDatePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(fromDate.format(DateTimeFormatter.ofPattern("MMM dd")))
                            }

                            Text(
                                text = "to",
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )

                            OutlinedButton(
                                onClick = { showToDatePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(toDate.format(DateTimeFormatter.ofPattern("MMM dd")))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = viewModel::loadReadings,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isLoading) "Loading..." else "Load Data")
                        }
                    }
                }

                // Chart
                if (readings.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Readings Chart (${readings.size} points)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            CartesianChartHost(
                                chart = rememberCartesianChart(
                                    rememberLineCartesianLayer(),
                                    startAxis = VerticalAxis.rememberStart(),
                                    bottomAxis = HorizontalAxis.rememberBottom()
                                ),
                                modelProducer = modelProducer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }
                } else if (!isLoading) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No readings in selected range",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Date pickers
    if (showFromDatePicker) {
        DatePickerDialog(
            selectedDate = fromDate,
            onDateSelected = {
                viewModel.setFromDate(it)
                showFromDatePicker = false
            },
            onDismiss = { showFromDatePicker = false }
        )
    }

    if (showToDatePicker) {
        DatePickerDialog(
            selectedDate = toDate,
            onDateSelected = {
                viewModel.setToDate(it)
                showToDatePicker = false
            },
            onDismiss = { showToDatePicker = false }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Sensor") },
            text = { Text("Are you sure you want to delete this sensor? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSensor { onBack() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        onDateSelected(date)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
