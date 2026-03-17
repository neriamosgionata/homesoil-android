package com.homesoil.app.ui.screens.flows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesoil.app.data.models.*
import com.homesoil.app.data.repository.HomesoilRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class FlowsViewModel(
    private val repository: HomesoilRepository
) : ViewModel() {

    val flows: StateFlow<Map<Int, DeviceFlow>> = repository.flows

    fun toggleFlow(flowId: Int, enabled: Boolean) {
        repository.toggleFlow(flowId, enabled)
    }

    fun removeFlow(flowId: Int) {
        repository.removeFlow(flowId)
    }
}

class FlowEditorViewModel(
    private val repository: HomesoilRepository,
    private val flowId: Int?
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val sensors: StateFlow<Map<Int, Sensor>> = repository.sensors
    val actuators: StateFlow<Map<Int, Actuator>> = repository.actuators
    val lastSensorReads: StateFlow<Map<Int, SensorRead>> = repository.lastSensorReads

    private val existingFlow: StateFlow<DeviceFlow?> = repository.flows.map { flows ->
        flowId?.let { flows[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _title = MutableStateFlow("New Flow")
    val title: StateFlow<String> = _title

    private val _nodes = MutableStateFlow<List<FlowNode>>(emptyList())
    val nodes: StateFlow<List<FlowNode>> = _nodes

    private val _edges = MutableStateFlow<List<FlowEdge>>(emptyList())
    val edges: StateFlow<List<FlowEdge>> = _edges

    private val _enabled = MutableStateFlow(false)

    val isNewFlow = flowId == null

    private var nodeIdCounter = 0

    init {
        if (flowId != null) {
            viewModelScope.launch {
                existingFlow.filterNotNull().first().let { flow ->
                    _title.value = flow.title
                    _enabled.value = flow.enabled
                    try {
                        val graph = json.decodeFromString<FlowGraph>(flow.graph)
                        _nodes.value = graph.nodes
                        _edges.value = graph.edges
                        nodeIdCounter = graph.nodes.size
                    } catch (e: Exception) {
                        android.util.Log.e("FlowEditor", "Error loading graph", e)
                    }
                }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun addNode(type: FlowNodeType, x: Float, y: Float) {
        val id = "${type.key}_${System.currentTimeMillis()}_${nodeIdCounter++}"
        val data = when (type) {
            FlowNodeType.SENSOR_INPUT -> FlowNodeData(sensorId = null)
            FlowNodeType.ACTUATOR_OUTPUT -> FlowNodeData(actuatorId = null, pulse = false)
            FlowNodeType.COMPARISON -> FlowNodeData(operator = ">")
            FlowNodeType.LOGIC_GATE -> FlowNodeData(operator = "AND")
            FlowNodeType.CONSTANT -> FlowNodeData(value = 0.0)
        }
        val node = FlowNode(id = id, type = type.key, position = FlowNodePosition(x, y), data = data)
        _nodes.value = _nodes.value + node
    }

    fun updateNodePosition(nodeId: String, x: Float, y: Float) {
        _nodes.value = _nodes.value.map { node ->
            if (node.id == nodeId) node.copy(position = FlowNodePosition(x, y)) else node
        }
    }

    fun updateNodeData(nodeId: String, data: FlowNodeData) {
        _nodes.value = _nodes.value.map { node ->
            if (node.id == nodeId) node.copy(data = data) else node
        }
    }

    fun removeNode(nodeId: String) {
        _nodes.value = _nodes.value.filter { it.id != nodeId }
        _edges.value = _edges.value.filter { it.source != nodeId && it.target != nodeId }
    }

    fun addEdge(sourceId: String, sourceHandle: String?, targetId: String, targetHandle: String?) {
        val existing = _edges.value.any {
            it.source == sourceId && it.sourceHandle == sourceHandle &&
                    it.target == targetId && it.targetHandle == targetHandle
        }
        if (existing || sourceId == targetId) return

        val edge = FlowEdge(
            id = "edge_${System.currentTimeMillis()}_${_edges.value.size}",
            source = sourceId,
            sourceHandle = sourceHandle,
            target = targetId,
            targetHandle = targetHandle
        )
        _edges.value = _edges.value + edge
    }

    fun removeEdge(edgeId: String) {
        _edges.value = _edges.value.filter { it.id != edgeId }
    }

    fun save(onSaved: () -> Unit) {
        if (_title.value.isBlank()) return

        val graph = FlowGraph(nodes = _nodes.value, edges = _edges.value)
        val graphJson = json.encodeToString(FlowGraph.serializer(), graph)

        if (flowId == null) {
            repository.addFlow(_title.value, graphJson)
        } else {
            repository.modifyFlow(flowId, _title.value, graphJson, _enabled.value)
        }
        onSaved()
    }

    fun delete(onDeleted: () -> Unit) {
        flowId?.let {
            repository.removeFlow(it)
            onDeleted()
        }
    }
}
