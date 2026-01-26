package com.homesoil.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homesoil.app.data.models.Sensor
import com.homesoil.app.data.models.SensorRead
import com.homesoil.app.data.models.SensorType
import com.homesoil.app.ui.theme.*
import com.homesoil.app.util.DateUtils

@Composable
fun SensorCard(
    sensor: Sensor,
    lastRead: SensorRead?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = getSensorIcon(sensor.sensorType),
                        contentDescription = null,
                        tint = getSensorColor(sensor.sensorType),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sensor.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OnlineIndicator(online = sensor.online)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = lastRead?.sensorValue ?: "--",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = sensor.sensorType.unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (lastRead != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = DateUtils.formatDisplayDateTime(lastRead.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sensor.sensorType.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OnlineIndicator(
    online: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (online) OnlineGreen else OfflineRed)
    )
}

fun getSensorIcon(sensorType: SensorType): ImageVector {
    return when (sensorType) {
        SensorType.TEMPERATURE -> Icons.Default.Thermostat
        SensorType.HUMIDITY -> Icons.Default.WaterDrop
        SensorType.PRESSURE -> Icons.Default.Speed
        SensorType.CURRENT -> Icons.Default.ElectricBolt
        SensorType.WIND_SPEED -> Icons.Default.Air
        SensorType.WIND_DIRECTION -> Icons.Default.Explore
        SensorType.RAIN -> Icons.Default.Umbrella
        SensorType.UV -> Icons.Default.WbSunny
        SensorType.SOLAR_RADIATION -> Icons.Default.LightMode
        SensorType.UNKNOWN -> Icons.Default.Sensors
    }
}

fun getSensorColor(sensorType: SensorType): androidx.compose.ui.graphics.Color {
    return when (sensorType) {
        SensorType.TEMPERATURE -> TemperatureColor
        SensorType.HUMIDITY -> HumidityColor
        SensorType.PRESSURE -> PressureColor
        SensorType.CURRENT -> CurrentColor
        SensorType.WIND_SPEED -> WindColor
        SensorType.WIND_DIRECTION -> WindColor
        SensorType.RAIN -> RainColor
        SensorType.UV -> UVColor
        SensorType.SOLAR_RADIATION -> SolarColor
        SensorType.UNKNOWN -> DarkOnSurfaceVariant
    }
}
