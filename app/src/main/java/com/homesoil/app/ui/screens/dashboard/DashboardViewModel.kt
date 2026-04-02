package com.homesoil.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesoil.app.data.models.Actuator
import com.homesoil.app.data.models.DashboardMessage
import com.homesoil.app.data.models.Sensor
import com.homesoil.app.data.models.SensorRead
import com.homesoil.app.data.repository.HomesoilRepository
import com.homesoil.app.network.ConnectionState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: HomesoilRepository
) : ViewModel() {

    val sensors: StateFlow<Map<Int, Sensor>> = repository.sensors
    val lastSensorReads: StateFlow<Map<Int, SensorRead>> = repository.lastSensorReads
    val actuators: StateFlow<Map<Int, Actuator>> = repository.actuators

    val connectionState = repository.connectionState
    val isConnected = connectionState.map { it == ConnectionState.CONNECTED }

    private val _messages = MutableSharedFlow<DashboardMessage>()
    val messages: SharedFlow<DashboardMessage> = _messages

    init {
        viewModelScope.launch {
            repository.messages.collect { message ->
                _messages.emit(message)
            }
        }
    }

    fun toggleActuator(actuatorId: Int) {
        repository.toggleActuator(actuatorId)
    }

    fun pulseActuator(actuatorId: Int) {
        repository.pulseActuator(actuatorId)
    }

    fun intermittentActuator(actuatorId: Int, onMs: Int, offMs: Int) {
        repository.intermittentActuator(actuatorId, onMs, offMs)
    }

    fun stopIntermittentActuator(actuatorId: Int) {
        repository.stopIntermittentActuator(actuatorId)
    }

    fun renameActuator(actuatorId: Int, name: String) {
        repository.renameActuator(actuatorId, name)
    }

    fun removeActuator(actuatorId: Int) {
        repository.removeActuator(actuatorId)
    }

    fun disconnect() {
        repository.disconnect()
    }
}
