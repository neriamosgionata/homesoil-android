package com.homesoil.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Actuator(
    val id: Int,
    val name: String?,
    @SerialName("ip_address")
    val ipAddress: String,
    val port: Int,
    val online: Boolean,
    val state: Boolean,
    val pulse: Boolean,
    val intermittent: Boolean = false,
    @SerialName("intermittent_on_ms")
    val intermittentOnMs: Int = 0,
    @SerialName("intermittent_off_ms")
    val intermittentOffMs: Int = 0,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    val displayName: String
        get() = name ?: "Actuator $id"
}
