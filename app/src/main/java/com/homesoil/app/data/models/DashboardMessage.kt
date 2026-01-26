package com.homesoil.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class DashboardMessage(
    val message: String,
    val type: String // "success", "error", "warning", "info"
) {
    val messageType: MessageType
        get() = MessageType.fromString(type)
}

enum class MessageType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO;

    companion object {
        fun fromString(value: String): MessageType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: INFO
        }
    }
}
