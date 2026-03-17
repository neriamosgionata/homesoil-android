package com.homesoil.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceFlow(
    val id: Int,
    val title: String,
    val graph: String,
    val enabled: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class FlowNodePosition(
    val x: Float,
    val y: Float
)

@Serializable
data class FlowNodeData(
    @SerialName("sensor_id")
    val sensorId: Int? = null,
    @SerialName("actuator_id")
    val actuatorId: Int? = null,
    val pulse: Boolean? = null,
    val operator: String? = null,
    val value: Double? = null
)

@Serializable
data class FlowNode(
    val id: String,
    val type: String,
    val position: FlowNodePosition,
    val data: FlowNodeData
)

@Serializable
data class FlowEdge(
    val id: String,
    val source: String,
    val sourceHandle: String? = null,
    val target: String,
    val targetHandle: String? = null
)

@Serializable
data class FlowGraph(
    val nodes: List<FlowNode>,
    val edges: List<FlowEdge>
)

enum class FlowNodeType(val key: String, val label: String) {
    SENSOR_INPUT("sensor_input", "Sensor"),
    ACTUATOR_OUTPUT("actuator_output", "Actuator"),
    COMPARISON("comparison", "Compare"),
    LOGIC_GATE("logic_gate", "Logic"),
    CONSTANT("constant", "Constant");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}
