package com.homesoil.app.ui.screens.sensor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesoil.app.data.models.Sensor
import com.homesoil.app.data.models.SensorRead
import com.homesoil.app.data.repository.HomesoilRepository
import com.homesoil.app.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class SensorDetailViewModel(
    private val repository: HomesoilRepository,
    private val sensorId: Int
) : ViewModel() {

    val sensor: StateFlow<Sensor?> = repository.sensors.map { sensors ->
        sensors[sensorId]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastRead: StateFlow<SensorRead?> = repository.lastSensorReads.map { reads ->
        reads[sensorId]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val readings: StateFlow<List<SensorRead>> = repository.sensorReadings

    private val _fromDate = MutableStateFlow(DateUtils.oneWeekAgo())
    val fromDate: StateFlow<LocalDate> = _fromDate

    private val _toDate = MutableStateFlow(DateUtils.today())
    val toDate: StateFlow<LocalDate> = _toDate

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _editingName = MutableStateFlow(false)
    val editingName: StateFlow<Boolean> = _editingName

    private val _newName = MutableStateFlow("")
    val newName: StateFlow<String> = _newName

    init {
        loadReadings()
    }

    fun setFromDate(date: LocalDate) {
        _fromDate.value = date
    }

    fun setToDate(date: LocalDate) {
        _toDate.value = date
    }

    fun loadReadings() {
        _isLoading.value = true
        repository.getSensorReadings(
            sensorId = sensorId,
            fromDate = DateUtils.formatDate(_fromDate.value),
            toDate = DateUtils.formatDate(_toDate.value)
        )
        viewModelScope.launch {
            // Watch for readings to stop loading indicator
            readings.first { it.isNotEmpty() || !_isLoading.value }
            _isLoading.value = false
        }
    }

    fun startEditingName() {
        _newName.value = sensor.value?.displayName ?: ""
        _editingName.value = true
    }

    fun cancelEditingName() {
        _editingName.value = false
    }

    fun updateNewName(name: String) {
        _newName.value = name
    }

    fun saveName() {
        if (_newName.value.isNotBlank()) {
            repository.renameSensor(sensorId, _newName.value)
        }
        _editingName.value = false
    }

    fun deleteSensor(onDeleted: () -> Unit) {
        repository.removeSensor(sensorId)
        onDeleted()
    }
}
