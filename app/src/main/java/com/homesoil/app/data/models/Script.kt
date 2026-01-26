package com.homesoil.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Script(
    val id: Int,
    val title: String,
    val code: String,
    val schedule: String? = null,
    val status: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    val isRunning: Boolean
        get() = status == 1

    val hasSchedule: Boolean
        get() = !schedule.isNullOrBlank()
}

@Serializable
data class NewScript(
    val title: String,
    val code: String,
    val schedule: String? = null
)

@Serializable
data class ModifyScript(
    val id: Int,
    val title: String,
    val code: String,
    val schedule: String? = null
)
