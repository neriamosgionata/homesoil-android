package com.homesoil.app.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homesoil.app.data.models.MessageType
import com.homesoil.app.network.ConnectionState
import com.homesoil.app.ui.components.ActuatorCard
import com.homesoil.app.ui.components.SensorCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onSensorClick: (Int) -> Unit,
    onScriptsClick: () -> Unit,
    onFlowsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDisconnected: () -> Unit
) {
    val sensors by viewModel.sensors.collectAsState()
    val lastSensorReads by viewModel.lastSensorReads.collectAsState()
    val actuators by viewModel.actuators.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
            onDisconnected()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message.message,
                    duration = when (message.messageType) {
                        MessageType.ERROR -> SnackbarDuration.Long
                        else -> SnackbarDuration.Short
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Homesoil") },
                actions = {
                    IconButton(onClick = onFlowsClick) {
                        Icon(Icons.Default.AccountTree, contentDescription = "Flows")
                    }
                    IconButton(onClick = onScriptsClick) {
                        Icon(Icons.Default.Code, contentDescription = "Scripts")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = {
                        viewModel.disconnect()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Disconnect")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onScriptsClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Code, contentDescription = "Scripts")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sensors (${sensors.size})") },
                    icon = { Icon(Icons.Default.Sensors, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Actuators (${actuators.size})") },
                    icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> SensorsTab(
                    sensors = sensors,
                    lastSensorReads = lastSensorReads,
                    onSensorClick = onSensorClick
                )
                1 -> ActuatorsTab(
                    actuators = actuators,
                    onToggle = viewModel::toggleActuator,
                    onPulse = viewModel::pulseActuator,
                    onRename = viewModel::renameActuator
                )
            }
        }
    }
}

@Composable
private fun SensorsTab(
    sensors: Map<Int, com.homesoil.app.data.models.Sensor>,
    lastSensorReads: Map<Int, com.homesoil.app.data.models.SensorRead>,
    onSensorClick: (Int) -> Unit
) {
    if (sensors.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Sensors,
            title = "No Sensors",
            message = "Waiting for sensors to connect..."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sensors.values.toList(), key = { it.id }) { sensor ->
                SensorCard(
                    sensor = sensor,
                    lastRead = lastSensorReads[sensor.id],
                    onClick = { onSensorClick(sensor.id) }
                )
            }
        }
    }
}

@Composable
private fun ActuatorsTab(
    actuators: Map<Int, com.homesoil.app.data.models.Actuator>,
    onToggle: (Int) -> Unit,
    onPulse: (Int) -> Unit,
    onRename: (Int, String) -> Unit
) {
    if (actuators.isEmpty()) {
        EmptyState(
            icon = Icons.Default.PowerSettingsNew,
            title = "No Actuators",
            message = "Waiting for actuators to connect..."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(actuators.values.toList(), key = { it.id }) { actuator ->
                ActuatorCard(
                    actuator = actuator,
                    onToggle = { onToggle(actuator.id) },
                    onPulse = { onPulse(actuator.id) },
                    onRename = { name -> onRename(actuator.id, name) }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
