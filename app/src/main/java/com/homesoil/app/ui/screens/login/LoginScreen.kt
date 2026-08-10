package com.homesoil.app.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.homesoil.app.ui.theme.HomesoilGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onConnected: () -> Unit
) {
    val token by viewModel.token.collectAsState()
    val pin by viewModel.pin.collectAsState()
    val serverHost by viewModel.serverHost.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val error by viewModel.error.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState(initial = false)
    val isConnected by viewModel.isConnected.collectAsState(initial = false)
    val isScanning by viewModel.isScanning.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()

    var showToken by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            onConnected()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Grass,
                contentDescription = null,
                tint = HomesoilGreen,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Homesoil",
                style = MaterialTheme.typography.headlineLarge,
                color = HomesoilGreen
            )

            Text(
                text = "IoT Infrastructure Management",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Server Address with scan button
            OutlinedTextField(
                value = serverHost,
                onValueChange = viewModel::updateServerHost,
                label = { Text("Server Address") },
                placeholder = { Text("192.168.1.100") },
                leadingIcon = {
                    Icon(Icons.Default.Dns, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(
                        onClick = viewModel::scanNetwork,
                        enabled = !isScanning && !isConnecting
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.WifiFind,
                                contentDescription = "Scan network"
                            )
                        }
                    }
                },
                singleLine = true,
                enabled = !isConnecting,
                modifier = Modifier.fillMaxWidth()
            )

            // Discovered servers list
            AnimatedVisibility(visible = discoveredServers.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Found servers:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        discoveredServers.forEach { server ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectServer(server) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Computer,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = HomesoilGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = server,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (server == serverHost) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(18.dp),
                                        tint = HomesoilGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                enabled = !isConnecting,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (token.isBlank()) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = viewModel::updatePin,
                    label = { Text("Pairing PIN") },
                    placeholder = { Text("Enter the PIN shown on your server") },
                    leadingIcon = {
                        Icon(Icons.Default.Pin, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    enabled = !isConnecting,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
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
                    enabled = !isConnecting,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = viewModel::connect,
                enabled = !isConnecting && serverHost.isNotBlank() &&
                    (token.isNotBlank() || pin.isNotBlank()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting...")
                } else {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (token.isBlank()) "Pair" else "Connect")
                }
            }

            if (token.isNotBlank()) {
                TextButton(
                    onClick = viewModel::clearPin,
                    enabled = !isConnecting
                ) {
                    Text("Pair with new PIN")
                }
            }
        }
    }
}
