package com.homesoil.app.ui.screens.flows

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.homesoil.app.data.models.*
import com.homesoil.app.ui.theme.*

private val NODE_WIDTH = 160.dp
private val HANDLE_SIZE = 14.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowEditorScreen(
    viewModel: FlowEditorViewModel,
    onBack: () -> Unit
) {
    val title by viewModel.title.collectAsState()
    val nodes by viewModel.nodes.collectAsState()
    val edges by viewModel.edges.collectAsState()
    val sensors by viewModel.sensors.collectAsState()
    val actuators by viewModel.actuators.collectAsState()
    val lastSensorReads by viewModel.lastSensorReads.collectAsState()
    val density = LocalDensity.current

    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var showNodePicker by remember { mutableStateOf(false) }
    var showTitleDialog by remember { mutableStateOf(false) }
    var connectingFrom by remember { mutableStateOf<Pair<String, String?>?>(null) }

    // Pan offset in px
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Measured node body heights in dp for handle alignment
    val nodeHeightsDp = remember { mutableStateMapOf<String, Dp>() }

    // Live drag offsets in dp — shared so edges update during drag
    val dragOffsets = remember { mutableStateMapOf<String, Offset>() }

    if (showTitleDialog) {
        var editTitle by remember { mutableStateOf(title) }
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text("Flow Title") },
            text = {
                OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.updateTitle(editTitle); showTitleDialog = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showNodePicker) {
        NodePickerDialog(
            onDismiss = { showNodePicker = false },
            onSelect = { type ->
                val cx = with(density) { (-panX).toDp().value + 80f }
                val cy = with(density) { (-panY).toDp().value + 200f }
                viewModel.addNode(type, cx.coerceAtLeast(20f), cy.coerceAtLeast(20f))
                showNodePicker = false
            }
        )
    }

    selectedNodeId?.let { nodeId ->
        nodes.find { it.id == nodeId }?.let { node ->
            NodeConfigSheet(
                node = node,
                sensors = sensors,
                actuators = actuators,
                lastSensorReads = lastSensorReads,
                onUpdateData = { data -> viewModel.updateNodeData(nodeId, data) },
                onDelete = { viewModel.removeNode(nodeId); selectedNodeId = null },
                onDismiss = { selectedNodeId = null }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title, modifier = Modifier.clickable { showTitleDialog = true },
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (!viewModel.isNewFlow) {
                        IconButton(onClick = { viewModel.delete(onBack) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = OfflineRed)
                        }
                    }
                    IconButton(onClick = { viewModel.save(onBack) }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNodePicker = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add Node")
            }
        }
    ) { padding ->
        val nodeWidthPx = with(density) { NODE_WIDTH.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clipToBounds()
        ) {
            // Background layer: grid + edges + pan gesture
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panX += dragAmount.x
                            panY += dragAmount.y
                        }
                    }
            ) {
                // Apply pan
                val px = panX
                val py = panY

                // Grid dots
                val gridSpacing = 40.dp.toPx()
                val startX = (((-px) / gridSpacing).toInt() - 1).coerceAtLeast(0)
                val startY = (((-py) / gridSpacing).toInt() - 1).coerceAtLeast(0)
                val endX = startX + (size.width / gridSpacing).toInt() + 2
                val endY = startY + (size.height / gridSpacing).toInt() + 2
                for (gx in startX..endX) {
                    for (gy in startY..endY) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.06f),
                            radius = 1.5f,
                            center = Offset(gx * gridSpacing + px, gy * gridSpacing + py)
                        )
                    }
                }

                // Draw edges
                for (edge in edges) {
                    val srcNode = nodes.find { it.id == edge.source } ?: continue
                    val tgtNode = nodes.find { it.id == edge.target } ?: continue

                    val srcDrag = dragOffsets[srcNode.id] ?: Offset.Zero
                    val tgtDrag = dragOffsets[tgtNode.id] ?: Offset.Zero

                    val srcHDp = nodeHeightsDp[srcNode.id] ?: 50.dp
                    val tgtHDp = nodeHeightsDp[tgtNode.id] ?: 50.dp
                    val srcHPx = srcHDp.toPx()
                    val tgtHPx = tgtHDp.toPx()

                    // Source: right edge of node, vertically centered (+ drag offset in dp)
                    val srcX = (srcNode.position.x + srcDrag.x).dp.toPx() + nodeWidthPx + px
                    val srcY = (srcNode.position.y + srcDrag.y).dp.toPx() + srcHPx / 2f + py

                    // Target: left edge, at specific handle (+ drag offset in dp)
                    val handleFraction = getHandleFraction(tgtNode, edge.targetHandle)
                    val tgtX = (tgtNode.position.x + tgtDrag.x).dp.toPx() + px
                    val tgtY = (tgtNode.position.y + tgtDrag.y).dp.toPx() + tgtHPx * handleFraction + py

                    val path = Path().apply {
                        moveTo(srcX, srcY)
                        val dx = ((tgtX - srcX) * 0.4f).coerceAtLeast(30.dp.toPx())
                        cubicTo(srcX + dx, srcY, tgtX - dx, tgtY, tgtX, tgtY)
                    }
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.5f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Nodes layer: each node positioned with graphicsLayer (no recomposition on pan)
            for (node in nodes) {
                key(node.id) {
                    DraggableFlowNode(
                        node = node,
                        panX = panX,
                        panY = panY,
                        sensors = sensors,
                        actuators = actuators,
                        lastSensorReads = lastSensorReads,
                        isSelected = node.id == selectedNodeId,
                        isConnecting = connectingFrom != null,
                        onTap = { selectedNodeId = node.id },
                        onDragUpdate = { dx, dy ->
                            dragOffsets[node.id] = Offset(dx, dy)
                        },
                        onDragEnd = { newX, newY ->
                            dragOffsets.remove(node.id)
                            viewModel.updateNodePosition(node.id, newX, newY)
                        },
                        onOutputTap = { connectingFrom = Pair(node.id, "output") },
                        onInputTap = { handle ->
                            connectingFrom?.let { (srcId, srcHandle) ->
                                viewModel.addEdge(srcId, srcHandle, node.id, handle)
                                connectingFrom = null
                            }
                        },
                        onHeightMeasured = { dp -> nodeHeightsDp[node.id] = dp }
                    )
                }
            }

            // Connection mode banner
            if (connectingFrom != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = NodeComparisonColor.copy(alpha = 0.9f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tap an input to connect", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { connectingFrom = null }) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun getHandleFraction(node: FlowNode, handle: String?): Float {
    val type = FlowNodeType.fromKey(node.type)
    return when {
        type == FlowNodeType.COMPARISON && handle == "a" -> 0.35f
        type == FlowNodeType.COMPARISON && handle == "b" -> 0.65f
        type == FlowNodeType.LOGIC_GATE && handle == "a" ->
            if (node.data.operator == "NOT") 0.5f else 0.35f
        type == FlowNodeType.LOGIC_GATE && handle == "b" -> 0.65f
        else -> 0.5f
    }
}

private fun nodeColorFor(type: FlowNodeType?): Color = when (type) {
    FlowNodeType.SENSOR_INPUT -> NodeSensorColor
    FlowNodeType.ACTUATOR_OUTPUT -> NodeActuatorColor
    FlowNodeType.COMPARISON -> NodeComparisonColor
    FlowNodeType.LOGIC_GATE -> NodeLogicColor
    FlowNodeType.CONSTANT -> NodeConstantColor
    null -> Color.Gray
}

@Composable
private fun DraggableFlowNode(
    node: FlowNode,
    panX: Float,
    panY: Float,
    sensors: Map<Int, Sensor>,
    actuators: Map<Int, Actuator>,
    lastSensorReads: Map<Int, SensorRead>,
    isSelected: Boolean,
    isConnecting: Boolean,
    onTap: () -> Unit,
    onDragUpdate: (Float, Float) -> Unit,
    onDragEnd: (Float, Float) -> Unit,
    onOutputTap: () -> Unit,
    onInputTap: (String?) -> Unit,
    onHeightMeasured: (Dp) -> Unit
) {
    val density = LocalDensity.current
    val nodeType = FlowNodeType.fromKey(node.type)
    val nodeColor = nodeColorFor(nodeType)

    // Local drag offset in dp — accumulates during drag, resets on end
    var localDx by remember { mutableFloatStateOf(0f) }
    var localDy by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    var bodyHeightDp by remember { mutableStateOf(50.dp) }

    // Final position = ViewModel position + local drag offset, in dp
    val posXDp = node.position.x + localDx
    val posYDp = node.position.y + localDy

    Box(
        modifier = Modifier
            .graphicsLayer {
                // Position = node dp position converted to px + pan offset
                translationX = with(density) { posXDp.dp.toPx() } + panX
                translationY = with(density) { posYDp.dp.toPx() } + panY
            }
            .width(NODE_WIDTH)
            .pointerInput(node.id) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localDx += with(density) { dragAmount.x.toDp().value }
                        localDy += with(density) { dragAmount.y.toDp().value }
                        onDragUpdate(localDx, localDy)
                    },
                    onDragEnd = {
                        isDragging = false
                        val newX = (node.position.x + localDx).coerceAtLeast(0f)
                        val newY = (node.position.y + localDy).coerceAtLeast(0f)
                        localDx = 0f
                        localDy = 0f
                        onDragEnd(newX, newY)
                    },
                    onDragCancel = {
                        isDragging = false
                        val newX = (node.position.x + localDx).coerceAtLeast(0f)
                        val newY = (node.position.y + localDy).coerceAtLeast(0f)
                        localDx = 0f
                        localDy = 0f
                        onDragEnd(newX, newY)
                    }
                )
            }
            .pointerInput(node.id) {
                detectTapGestures { onTap() }
            }
    ) {
        // Node body
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(2.dp, nodeColor)
            else
                androidx.compose.foundation.BorderStroke(1.dp, nodeColor.copy(alpha = 0.5f)),
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    val h = with(density) { size.height.toDp() }
                    bodyHeightDp = h
                    onHeightMeasured(h)
                }
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(nodeColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(nodeType?.label ?: node.type, style = MaterialTheme.typography.labelSmall, color = nodeColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                NodeContent(node, nodeType, sensors, actuators, lastSensorReads)
            }
        }

        // Input handles (left side)
        if (nodeType == FlowNodeType.ACTUATOR_OUTPUT || nodeType == FlowNodeType.COMPARISON || nodeType == FlowNodeType.LOGIC_GATE) {
            val handles = when (nodeType) {
                FlowNodeType.COMPARISON -> listOf("a" to 0.35f, "b" to 0.65f)
                FlowNodeType.LOGIC_GATE ->
                    if (node.data.operator == "NOT") listOf("a" to 0.5f) else listOf("a" to 0.35f, "b" to 0.65f)
                else -> listOf("trigger" to 0.5f)
            }
            for ((handleId, fraction) in handles) {
                HandleDot(
                    color = if (isConnecting) nodeColor else nodeColor.copy(alpha = 0.7f),
                    modifier = Modifier
                        .offset(x = -(HANDLE_SIZE / 2), y = bodyHeightDp * fraction - HANDLE_SIZE / 2)
                        .size(HANDLE_SIZE)
                        .clickable { onInputTap(handleId) }
                )
            }
        }

        // Output handle (right side)
        if (nodeType != FlowNodeType.ACTUATOR_OUTPUT) {
            HandleDot(
                color = nodeColor,
                modifier = Modifier
                    .offset(x = NODE_WIDTH - HANDLE_SIZE / 2, y = bodyHeightDp * 0.5f - HANDLE_SIZE / 2)
                    .size(HANDLE_SIZE)
                    .clickable { onOutputTap() }
            )
        }
    }
}

@Composable
private fun HandleDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
    )
}

@Composable
private fun NodeContent(
    node: FlowNode,
    nodeType: FlowNodeType?,
    sensors: Map<Int, Sensor>,
    actuators: Map<Int, Actuator>,
    lastSensorReads: Map<Int, SensorRead>
) {
    when (nodeType) {
        FlowNodeType.SENSOR_INPUT -> {
            val sensor = node.data.sensorId?.let { sensors[it] }
            Text(sensor?.displayName ?: "Select sensor", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sensor != null) {
                val read = lastSensorReads[sensor.id]
                Text(read?.sensorValue ?: "—", style = MaterialTheme.typography.labelSmall,
                    color = if (sensor.online) OnlineGreen else OfflineRed)
            }
        }
        FlowNodeType.ACTUATOR_OUTPUT -> {
            val actuator = node.data.actuatorId?.let { actuators[it] }
            Text(actuator?.displayName ?: "Select actuator", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (actuator != null) {
                Text(if (actuator.state) "ON" else "OFF", style = MaterialTheme.typography.labelSmall,
                    color = if (actuator.state) OnlineGreen else OfflineRed)
            }
        }
        FlowNodeType.COMPARISON -> {
            Text("A ${node.data.operator ?: ">"} B", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
        }
        FlowNodeType.LOGIC_GATE -> {
            Text(node.data.operator ?: "AND", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
        }
        FlowNodeType.CONSTANT -> {
            Text((node.data.value ?: 0.0).let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        null -> {}
    }
}

@Composable
private fun NodePickerDialog(onDismiss: () -> Unit, onSelect: (FlowNodeType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Node") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (type in FlowNodeType.entries) {
                    val color = nodeColorFor(type)
                    val icon = when (type) {
                        FlowNodeType.SENSOR_INPUT -> Icons.Default.Sensors
                        FlowNodeType.ACTUATOR_OUTPUT -> Icons.Default.PowerSettingsNew
                        FlowNodeType.COMPARISON -> Icons.Default.Compare
                        FlowNodeType.LOGIC_GATE -> Icons.Default.Hub
                        FlowNodeType.CONSTANT -> Icons.Default.Pin
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(type) },
                        shape = RoundedCornerShape(8.dp),
                        color = color.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(type.label, color = color)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun NodeConfigSheet(
    node: FlowNode,
    sensors: Map<Int, Sensor>,
    actuators: Map<Int, Actuator>,
    lastSensorReads: Map<Int, SensorRead>,
    onUpdateData: (FlowNodeData) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val nodeType = FlowNodeType.fromKey(node.type)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${nodeType?.label ?: "Node"}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (nodeType) {
                    FlowNodeType.SENSOR_INPUT -> {
                        Text("Select Sensor", style = MaterialTheme.typography.labelMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (sensor in sensors.values) {
                                val isSel = node.data.sensorId == sensor.id
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { onUpdateData(node.data.copy(sensorId = sensor.id)) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) NodeSensorColor.copy(alpha = 0.2f) else Color.Transparent,
                                    border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, NodeSensorColor) else null
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (sensor.online) OnlineGreen else OfflineRed))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(sensor.displayName, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            if (sensors.isEmpty()) Text("No sensors available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    FlowNodeType.ACTUATOR_OUTPUT -> {
                        Text("Select Actuator", style = MaterialTheme.typography.labelMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (actuator in actuators.values) {
                                val isSel = node.data.actuatorId == actuator.id
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { onUpdateData(node.data.copy(actuatorId = actuator.id)) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) NodeActuatorColor.copy(alpha = 0.2f) else Color.Transparent,
                                    border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, NodeActuatorColor) else null
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (actuator.online) OnlineGreen else OfflineRed))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(actuator.displayName, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            if (actuators.isEmpty()) Text("No actuators available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = node.data.pulse == true, onCheckedChange = { onUpdateData(node.data.copy(pulse = it)) })
                            Text("Pulse mode", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    FlowNodeType.COMPARISON -> {
                        Text("Operator", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (op in listOf(">", "<", ">=", "<=", "==", "!=")) {
                                FilterChip(selected = node.data.operator == op, onClick = { onUpdateData(node.data.copy(operator = op)) }, label = { Text(op) })
                            }
                        }
                    }
                    FlowNodeType.LOGIC_GATE -> {
                        Text("Gate Type", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (gate in listOf("AND", "OR", "NOT")) {
                                FilterChip(selected = node.data.operator == gate, onClick = { onUpdateData(node.data.copy(operator = gate)) }, label = { Text(gate) })
                            }
                        }
                    }
                    FlowNodeType.CONSTANT -> {
                        var valueText by remember { mutableStateOf((node.data.value ?: 0.0).toString()) }
                        OutlinedTextField(value = valueText, onValueChange = { newVal ->
                            valueText = newVal
                            newVal.toDoubleOrNull()?.let { onUpdateData(node.data.copy(value = it)) }
                        }, label = { Text("Value") }, singleLine = true)
                    }
                    null -> {}
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDelete) { Text("Delete", color = OfflineRed) } }
    )
}
