package com.homesoil.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SensorType(val displayName: String, val unit: String) {
    @SerialName("current")
    CURRENT("Current", "A"),

    @SerialName("temperature")
    TEMPERATURE("Temperature", "°C"),

    @SerialName("humidity")
    HUMIDITY("Humidity", "%"),

    @SerialName("pressure")
    PRESSURE("Pressure", "hPa"),

    @SerialName("wind_speed")
    WIND_SPEED("Wind Speed", "m/s"),

    @SerialName("wind_direction")
    WIND_DIRECTION("Wind Direction", "°"),

    @SerialName("rain")
    RAIN("Rain", "mm"),

    @SerialName("uv")
    UV("UV Index", ""),

    @SerialName("solar_radiation")
    SOLAR_RADIATION("Solar Radiation", "W/m²"),

    @SerialName("unknown")
    UNKNOWN("Unknown", "");

    companion object {
        fun fromString(value: String): SensorType {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: entries.find { it.serialName() == value }
                ?: UNKNOWN
        }

        private fun SensorType.serialName(): String {
            return this::class.java.getField(this.name)
                .getAnnotation(SerialName::class.java)?.value ?: this.name.lowercase()
        }
    }
}
