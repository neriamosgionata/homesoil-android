package com.homesoil.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SensorRead(
    val id: Int,
    @SerialName("sensor_id")
    val sensorId: Int,
    @SerialName("sensor_value")
    val sensorValue: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    val valueAsDouble: Double?
        get() = sensorValue.toDoubleOrNull()
}
