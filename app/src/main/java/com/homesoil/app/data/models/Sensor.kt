package com.homesoil.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Sensor(
    val id: Int,
    val name: String?,
    @SerialName("sensor_type")
    val sensorType: SensorType,
    @SerialName("ip_address")
    val ipAddress: String,
    val port: Int,
    val online: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    val displayName: String
        get() = name ?: "Sensor $id"
}
