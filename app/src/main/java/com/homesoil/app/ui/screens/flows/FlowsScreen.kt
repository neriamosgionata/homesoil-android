package com.homesoil.app.ui.screens.flows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.homesoil.app.ui.components.FlowCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowsScreen(
    viewModel: FlowsViewModel,
    onBack: () -> Unit,
    onFlowClick: (Int) -> Unit,
    onNewFlow: () -> Unit
) {
    val flows by viewModel.flows.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flows") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewFlow,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Flow")
            }
        }
    ) { padding ->
        if (flows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Flows",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create a visual flow to automate your devices",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(flows.values.toList(), key = { it.id }) { flow ->
                    FlowCard(
                        flow = flow,
                        onClick = { onFlowClick(flow.id) },
                        onToggle = { enabled -> viewModel.toggleFlow(flow.id, enabled) },
                        onDelete = { viewModel.removeFlow(flow.id) }
                    )
                }
            }
        }
    }
}
