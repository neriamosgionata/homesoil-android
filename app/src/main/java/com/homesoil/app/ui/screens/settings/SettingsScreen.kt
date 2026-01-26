package com.homesoil.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.homesoil.app.network.ConnectionState
import com.homesoil.app.ui.theme.HomesoilGreen
import com.homesoil.app.ui.theme.OfflineRed
import com.homesoil.app.ui.theme.OnlineGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val serverHost by viewModel.serverHost.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val token by viewModel.token.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var showToken by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Connection status card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (connectionState) {
                                ConnectionState.CONNECTED -> Icons.Default.Wifi
                                ConnectionState.CONNECTING -> Icons.Default.Sync
                                ConnectionState.ERROR -> Icons.Default.WifiOff
                                ConnectionState.DISCONNECTED -> Icons.Default.WifiOff
                            },
                            contentDescription = null,
                            tint = when (connectionState) {
                                ConnectionState.CONNECTED -> OnlineGreen
                                ConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary
                                ConnectionState.ERROR -> OfflineRed
                                ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Connection Status",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = when (connectionState) {
                                    ConnectionState.CONNECTED -> "Connected"
                                    ConnectionState.CONNECTING -> "Connecting..."
                                    ConnectionState.ERROR -> "Connection Error"
                                    ConnectionState.DISCONNECTED -> "Disconnected"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (connectionState == ConnectionState.CONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Server settings
            Text(
                text = "Server Configuration",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = serverHost,
                onValueChange = viewModel::updateServerHost,
                label = { Text("Server Address") },
                placeholder = { Text("192.168.1.100") },
                leadingIcon = {
                    Icon(Icons.Default.Dns, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = serverPort,
                onValueChange = viewModel::updateServerPort,
                label = { Text("Port") },
                placeholder = { Text("4000") },
                leadingIcon = {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = token,
                onValueChange = viewModel::updateToken,
                label = { Text("Authentication Token") },
                placeholder = { Text("Enter your token") },
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(
                            imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showToken) "Hide token" else "Show token"
                        )
                    }
                },
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Test result
            testResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (result) {
                            is TestResult.Success -> MaterialTheme.colorScheme.primaryContainer
                            is TestResult.Error -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (result) {
                                is TestResult.Success -> Icons.Default.CheckCircle
                                is TestResult.Error -> Icons.Default.Error
                            },
                            contentDescription = null,
                            tint = when (result) {
                                is TestResult.Success -> OnlineGreen
                                is TestResult.Error -> MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (result) {
                                is TestResult.Success -> "Connection successful"
                                is TestResult.Error -> result.message
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            OutlinedButton(
                onClick = viewModel::testConnection,
                enabled = !isTesting && serverHost.isNotBlank() && token.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing...")
                } else {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Connection")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.saveAndReconnect()
                    onBack()
                },
                enabled = serverHost.isNotBlank() && token.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HomesoilGreen)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Reconnect")
            }

            if (connectionState == ConnectionState.CONNECTED) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = viewModel::disconnect,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // About section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grass,
                            contentDescription = null,
                            tint = HomesoilGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Homesoil",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "IoT Infrastructure Management for smart home and agricultural automation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
