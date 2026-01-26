package com.homesoil.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.homesoil.app.data.models.*
import com.homesoil.app.network.ConnectionState
import com.homesoil.app.network.SocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "homesoil_settings")

class HomesoilRepository(private val context: Context) {
    private val socketManager = SocketManager()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Connection state
    val connectionState: StateFlow<ConnectionState> = socketManager.connectionState

    // Data from socket
    val sensors: StateFlow<Map<Int, Sensor>> = socketManager.sensors
    val lastSensorReads: StateFlow<Map<Int, SensorRead>> = socketManager.lastSensorReads
    val actuators: StateFlow<Map<Int, Actuator>> = socketManager.actuators
    val scripts: StateFlow<Map<Int, Script>> = socketManager.scripts
    val sensorReadings: StateFlow<List<SensorRead>> = socketManager.sensorReadings
    val messages: SharedFlow<DashboardMessage> = socketManager.messages
    val connectionError: SharedFlow<String> = socketManager.connectionError

    // Settings keys
    private object PrefsKeys {
        val SERVER_HOST = stringPreferencesKey("server_host")
        val SERVER_PORT = intPreferencesKey("server_port")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
    }

    // Settings flows
    val serverHost: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PrefsKeys.SERVER_HOST] ?: ""
    }

    val serverPort: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PrefsKeys.SERVER_PORT] ?: 4000
    }

    val authToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PrefsKeys.AUTH_TOKEN] ?: ""
    }

    suspend fun saveServerSettings(host: String, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.SERVER_HOST] = host
            prefs[PrefsKeys.SERVER_PORT] = port
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.AUTH_TOKEN] = token
        }
    }

    fun connect(serverUrl: String, token: String) {
        socketManager.connect(serverUrl, token)
    }

    fun disconnect() {
        socketManager.disconnect()
    }

    // Sensor operations
    fun getSensorReadings(sensorId: Int, fromDate: String, toDate: String) {
        socketManager.getSensorReadings(sensorId, fromDate, toDate)
    }

    fun renameSensor(sensorId: Int, name: String) {
        socketManager.renameSensor(sensorId, name)
    }

    fun removeSensor(sensorId: Int) {
        socketManager.removeSensor(sensorId)
    }

    // Actuator operations
    fun toggleActuator(actuatorId: Int) {
        socketManager.toggleActuator(actuatorId)
    }

    fun pulseActuator(actuatorId: Int) {
        socketManager.pulseActuator(actuatorId)
    }

    fun renameActuator(actuatorId: Int, name: String) {
        socketManager.renameActuator(actuatorId, name)
    }

    fun removeActuator(actuatorId: Int) {
        socketManager.removeActuator(actuatorId)
    }

    // Script operations
    fun getAllScripts() {
        socketManager.getAllScripts()
    }

    fun runScript(scriptId: Int) {
        socketManager.runScript(scriptId)
    }

    fun addScript(title: String, code: String) {
        socketManager.addScript(title, code)
    }

    fun modifyScript(scriptId: Int, title: String, code: String) {
        socketManager.modifyScript(scriptId, title, code)
    }

    fun removeScript(scriptId: Int) {
        socketManager.removeScript(scriptId)
    }

    fun addScriptSchedule(scriptId: Int, schedule: String) {
        socketManager.addScriptSchedule(scriptId, schedule)
    }

    fun removeScriptSchedule(scriptId: Int) {
        socketManager.removeScriptSchedule(scriptId)
    }
}
