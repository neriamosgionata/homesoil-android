package com.homesoil.app.network

import android.util.Log
import com.homesoil.app.data.models.*
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

class SocketManager {
    private var socket: Socket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // Connection state
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // Data flows
    private val _sensors = MutableStateFlow<Map<Int, Sensor>>(emptyMap())
    val sensors: StateFlow<Map<Int, Sensor>> = _sensors

    private val _lastSensorReads = MutableStateFlow<Map<Int, SensorRead>>(emptyMap())
    val lastSensorReads: StateFlow<Map<Int, SensorRead>> = _lastSensorReads

    private val _actuators = MutableStateFlow<Map<Int, Actuator>>(emptyMap())
    val actuators: StateFlow<Map<Int, Actuator>> = _actuators

    private val _scripts = MutableStateFlow<Map<Int, Script>>(emptyMap())
    val scripts: StateFlow<Map<Int, Script>> = _scripts

    private val _sensorReadings = MutableStateFlow<List<SensorRead>>(emptyList())
    val sensorReadings: StateFlow<List<SensorRead>> = _sensorReadings

    private val _messages = MutableSharedFlow<DashboardMessage>()
    val messages: SharedFlow<DashboardMessage> = _messages

    private val _connectionError = MutableSharedFlow<String>()
    val connectionError: SharedFlow<String> = _connectionError

    fun connect(serverUrl: String, token: String) {
        disconnect()

        try {
            val options = IO.Options().apply {
                transports = arrayOf("websocket", "polling")
                auth = mapOf("token" to token)
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 10000
            }

            socket = IO.socket(serverUrl, options).apply {
                setupConnectionListeners()
                setupSensorListeners()
                setupActuatorListeners()
                setupScriptListeners()
                setupMessageListeners()
                connect()
            }

            _connectionState.value = ConnectionState.CONNECTING
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create socket", e)
            scope.launch {
                _connectionError.emit("Failed to connect: ${e.message}")
            }
            _connectionState.value = ConnectionState.ERROR
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        clearData()
    }

    private fun clearData() {
        _sensors.value = emptyMap()
        _lastSensorReads.value = emptyMap()
        _actuators.value = emptyMap()
        _scripts.value = emptyMap()
        _sensorReadings.value = emptyList()
    }

    private fun Socket.setupConnectionListeners() {
        on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket connected")
            _connectionState.value = ConnectionState.CONNECTED
        }

        on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Socket disconnected")
            _connectionState.value = ConnectionState.DISCONNECTED
        }

        on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.firstOrNull()?.toString() ?: "Unknown error"
            Log.e(TAG, "Socket connection error: $error")
            scope.launch {
                _connectionError.emit(error)
            }
            _connectionState.value = ConnectionState.ERROR
        }
    }

    private fun Socket.setupSensorListeners() {
        on(SocketEvents.Listen.ALL_SENSORS) { args ->
            handleAllSensors(args)
        }

        on(SocketEvents.Listen.ALL_LAST_SENSOR_READINGS) { args ->
            handleAllLastSensorReadings(args)
        }

        on(SocketEvents.Listen.SENSOR_REGISTER) { args ->
            handleSensorRegister(args)
        }

        on(SocketEvents.Listen.SENSOR_UNREGISTER) { args ->
            handleSensorUnregister(args)
        }

        on(SocketEvents.Listen.SENSOR_NAME_CHANGE) { args ->
            handleSensorNameChange(args)
        }

        on(SocketEvents.Listen.SENSOR_READ) { args ->
            handleSensorRead(args)
        }

        on(SocketEvents.Listen.ALL_SENSOR_READINGS) { args ->
            handleAllSensorReadings(args)
        }

        on(SocketEvents.Listen.SENSOR_CHANGE_ONLINE) { args ->
            handleSensorChangeOnline(args)
        }
    }

    private fun Socket.setupActuatorListeners() {
        on(SocketEvents.Listen.ALL_ACTUATORS) { args ->
            handleAllActuators(args)
        }

        on(SocketEvents.Listen.ACTUATOR_REGISTER) { args ->
            handleActuatorRegister(args)
        }

        on(SocketEvents.Listen.ACTUATOR_UNREGISTER) { args ->
            handleActuatorUnregister(args)
        }

        on(SocketEvents.Listen.ACTUATOR_NAME_CHANGE) { args ->
            handleActuatorNameChange(args)
        }

        on(SocketEvents.Listen.ACTUATOR_STATE_CHANGE) { args ->
            handleActuatorStateChange(args)
        }

        on(SocketEvents.Listen.ACTUATOR_CHANGE_ONLINE) { args ->
            handleActuatorChangeOnline(args)
        }
    }

    private fun Socket.setupScriptListeners() {
        on(SocketEvents.Listen.ALL_SCRIPTS) { args ->
            handleAllScripts(args)
        }

        on(SocketEvents.Listen.SCRIPT_SAVED) { args ->
            handleScriptSaved(args)
        }

        on(SocketEvents.Listen.SCRIPT_DELETED) { args ->
            handleScriptDeleted(args)
        }

        on(SocketEvents.Listen.SCRIPT_MODIFIED) { args ->
            handleScriptModified(args)
        }

        on(SocketEvents.Listen.SCRIPT_STATUS_CHANGE) { args ->
            handleScriptStatusChange(args)
        }

        on(SocketEvents.Listen.SCRIPT_SCHEDULE_ADDED) { args ->
            handleScriptScheduleAdded(args)
        }

        on(SocketEvents.Listen.SCRIPT_SCHEDULE_REMOVED) { args ->
            handleScriptScheduleRemoved(args)
        }
    }

    private fun Socket.setupMessageListeners() {
        on(SocketEvents.Listen.MESSAGE_SENT) { args ->
            handleMessageSent(args)
        }
    }

    // Sensor event handlers
    private fun handleAllSensors(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val sensorsArray = data.getJSONArray("sensors")
            val sensorsMap = mutableMapOf<Int, Sensor>()

            for (i in 0 until sensorsArray.length()) {
                val sensorJson = sensorsArray.getJSONObject(i)
                val sensor = json.decodeFromString<Sensor>(sensorJson.toString())
                sensorsMap[sensor.id] = sensor
            }

            _sensors.value = sensorsMap
            Log.d(TAG, "Received ${sensorsMap.size} sensors")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing all sensors", e)
        }
    }

    private fun handleAllLastSensorReadings(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val readsArray = data.getJSONArray("reads")
            val readsMap = mutableMapOf<Int, SensorRead>()

            for (i in 0 until readsArray.length()) {
                val readJson = readsArray.getJSONObject(i)
                val read = json.decodeFromString<SensorRead>(readJson.toString())
                readsMap[read.sensorId] = read
            }

            _lastSensorReads.value = readsMap
            Log.d(TAG, "Received ${readsMap.size} last sensor reads")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing all last sensor readings", e)
        }
    }

    private fun handleSensorRegister(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val sensor = Sensor(
                id = data.getInt("sensor_id"),
                name = data.optString("sensor_name").takeIf { it.isNotEmpty() },
                sensorType = SensorType.fromString(data.getString("sensor_type")),
                ipAddress = data.getString("sensor_ip_address"),
                port = data.getInt("sensor_port"),
                online = data.getBoolean("online"),
                createdAt = data.getString("created_at")
            )
            _sensors.value = _sensors.value + (sensor.id to sensor)
            Log.d(TAG, "Sensor registered: ${sensor.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sensor register", e)
        }
    }

    private fun handleSensorUnregister(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val sensorId = data.getInt("sensor_id")
            _sensors.value = _sensors.value - sensorId
            _lastSensorReads.value = _lastSensorReads.value - sensorId
            Log.d(TAG, "Sensor unregistered: $sensorId")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sensor unregister", e)
        }
    }

    private fun handleSensorNameChange(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val sensorId = data.getInt("sensor_id")
            val newName = data.getString("name")

            _sensors.value[sensorId]?.let { sensor ->
                _sensors.value = _sensors.value + (sensorId to sensor.copy(name = newName))
            }
            Log.d(TAG, "Sensor name changed: $sensorId -> $newName")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sensor name change", e)
        }
    }

    private fun handleSensorRead(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val read = SensorRead(
                id = data.getInt("id"),
                sensorId = data.getInt("sensor_id"),
                sensorValue = data.getString("sensor_value"),
                createdAt = data.getString("created_at")
            )
            _lastSensorReads.value = _lastSensorReads.value + (read.sensorId to read)
            Log.d(TAG, "Sensor read: ${read.sensorId} = ${read.sensorValue}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sensor read", e)
        }
    }

    private fun handleAllSensorReadings(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val readsArray = data.getJSONArray("reads")
            val reads = mutableListOf<SensorRead>()

            for (i in 0 until readsArray.length()) {
                val readJson = readsArray.getJSONObject(i)
                val read = json.decodeFromString<SensorRead>(readJson.toString())
                reads.add(read)
            }

            _sensorReadings.value = reads.sortedBy { it.createdAt }
            Log.d(TAG, "Received ${reads.size} sensor readings")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing all sensor readings", e)
        }
    }

    private fun handleSensorChangeOnline(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val sensorId = data.getInt("sensor_id")
            val online = data.getBoolean("online")

            _sensors.value[sensorId]?.let { sensor ->
                _sensors.value = _sensors.value + (sensorId to sensor.copy(online = online))
            }
            Log.d(TAG, "Sensor online change: $sensorId -> $online")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sensor change online", e)
        }
    }

    // Actuator event handlers
    private fun handleAllActuators(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val actuatorsArray = data.getJSONArray("actuators")
            val actuatorsMap = mutableMapOf<Int, Actuator>()

            for (i in 0 until actuatorsArray.length()) {
                val actuatorJson = actuatorsArray.getJSONObject(i)
                val actuator = json.decodeFromString<Actuator>(actuatorJson.toString())
                actuatorsMap[actuator.id] = actuator
            }

            _actuators.value = actuatorsMap
            Log.d(TAG, "Received ${actuatorsMap.size} actuators")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing all actuators", e)
        }
    }

    private fun handleActuatorRegister(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val actuator = Actuator(
                id = data.getInt("actuator_id"),
                name = data.optString("actuator_name").takeIf { it.isNotEmpty() },
                ipAddress = data.getString("actuator_ip_address"),
                port = data.getInt("actuator_port"),
                online = data.getBoolean("online"),
                state = data.getBoolean("actuator_state"),
                pulse = data.getBoolean("actuator_pulse"),
                createdAt = data.getString("created_at")
            )
            _actuators.value = _actuators.value + (actuator.id to actuator)
            Log.d(TAG, "Actuator registered: ${actuator.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing actuator register", e)
        }
    }

    private fun handleActuatorUnregister(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val actuatorId = data.getInt("actuator_id")
            _actuators.value = _actuators.value - actuatorId
            Log.d(TAG, "Actuator unregistered: $actuatorId")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing actuator unregister", e)
        }
    }

    private fun handleActuatorNameChange(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val actuatorId = data.getInt("actuator_id")
            val newName = data.getString("name")

            _actuators.value[actuatorId]?.let { actuator ->
                _actuators.value = _actuators.value + (actuatorId to actuator.copy(name = newName))
            }
            Log.d(TAG, "Actuator name changed: $actuatorId -> $newName")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing actuator name change", e)
        }
    }

    private fun handleActuatorStateChange(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val actuatorId = data.getInt("actuator_id")
            val state = data.getBoolean("actuator_state")

            _actuators.value[actuatorId]?.let { actuator ->
                _actuators.value = _actuators.value + (actuatorId to actuator.copy(state = state))
            }
            Log.d(TAG, "Actuator state change: $actuatorId -> $state")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing actuator state change", e)
        }
    }

    private fun handleActuatorChangeOnline(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val actuatorId = data.getInt("actuator_id")
            val online = data.getBoolean("online")

            _actuators.value[actuatorId]?.let { actuator ->
                _actuators.value = _actuators.value + (actuatorId to actuator.copy(online = online))
            }
            Log.d(TAG, "Actuator online change: $actuatorId -> $online")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing actuator change online", e)
        }
    }

    // Script event handlers
    private fun handleAllScripts(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val scriptsArray = data.getJSONArray("scripts_array")
            val scriptsMap = mutableMapOf<Int, Script>()

            for (i in 0 until scriptsArray.length()) {
                val scriptJson = scriptsArray.getJSONObject(i)
                val script = json.decodeFromString<Script>(scriptJson.toString())
                scriptsMap[script.id] = script
            }

            _scripts.value = scriptsMap
            Log.d(TAG, "Received ${scriptsMap.size} scripts")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing all scripts", e)
        }
    }

    private fun handleScriptSaved(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val script = json.decodeFromString<Script>(data.toString())
            _scripts.value = _scripts.value + (script.id to script)
            Log.d(TAG, "Script saved: ${script.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing script saved", e)
        }
    }

    private fun handleScriptDeleted(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val scriptId = data.getInt("id")
            _scripts.value = _scripts.value - scriptId
            Log.d(TAG, "Script deleted: $scriptId")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing script deleted", e)
        }
    }

    private fun handleScriptModified(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val script = json.decodeFromString<Script>(data.toString())
            _scripts.value = _scripts.value + (script.id to script)
            Log.d(TAG, "Script modified: ${script.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing script modified", e)
        }
    }

    private fun handleScriptStatusChange(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val scriptId = data.getInt("id")
            val status = data.getInt("status")

            _scripts.value[scriptId]?.let { script ->
                _scripts.value = _scripts.value + (scriptId to script.copy(status = status))
            }
            Log.d(TAG, "Script status change: $scriptId -> $status")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing script status change", e)
        }
    }

    private fun handleScriptScheduleAdded(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val scriptId = data.getInt("id")
            val schedule = data.optString("schedule").takeIf { it.isNotEmpty() }

            _scripts.value[scriptId]?.let { script ->
                _scripts.value = _scripts.value + (scriptId to script.copy(schedule = schedule))
            }
            Log.d(TAG, "Script schedule added: $scriptId -> $schedule")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing script schedule added", e)
        }
    }

    private fun handleScriptScheduleRemoved(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val scriptId = data.getInt("id")

            _scripts.value[scriptId]?.let { script ->
                _scripts.value = _scripts.value + (scriptId to script.copy(schedule = null))
            }
            Log.d(TAG, "Script schedule removed: $scriptId")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing script schedule removed", e)
        }
    }

    private fun handleMessageSent(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val message = DashboardMessage(
                message = data.getString("message"),
                type = data.getString("type")
            )
            scope.launch {
                _messages.emit(message)
            }
            Log.d(TAG, "Message received: ${message.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message sent", e)
        }
    }

    // Emit methods - Sensors
    fun getSensorReadings(sensorId: Int, fromDate: String, toDate: String) {
        _sensorReadings.value = emptyList()
        socket?.emit(
            SocketEvents.Emit.GET_SENSOR_READINGS,
            JSONObject().apply {
                put("id", sensorId)
                put("from_date", fromDate)
                put("to_date", toDate)
            }
        )
    }

    fun renameSensor(sensorId: Int, name: String) {
        socket?.emit(
            SocketEvents.Emit.RENAME_SENSOR,
            JSONObject().apply {
                put("id", sensorId)
                put("name", name)
            }
        )
    }

    fun removeSensor(sensorId: Int) {
        socket?.emit(
            SocketEvents.Emit.REMOVE_SENSOR,
            JSONObject().apply {
                put("id", sensorId)
            }
        )
    }

    // Emit methods - Actuators
    fun toggleActuator(actuatorId: Int) {
        socket?.emit(SocketEvents.Emit.TOGGLE_ACTUATOR, actuatorId)
    }

    fun pulseActuator(actuatorId: Int) {
        socket?.emit(SocketEvents.Emit.PULSE_ACTUATOR, actuatorId)
    }

    fun renameActuator(actuatorId: Int, name: String) {
        socket?.emit(
            SocketEvents.Emit.RENAME_ACTUATOR,
            JSONObject().apply {
                put("id", actuatorId)
                put("name", name)
            }
        )
    }

    fun removeActuator(actuatorId: Int) {
        socket?.emit(
            SocketEvents.Emit.REMOVE_ACTUATOR,
            JSONObject().apply {
                put("id", actuatorId)
            }
        )
    }

    // Emit methods - Scripts
    fun getAllScripts() {
        socket?.emit(SocketEvents.Emit.GET_ALL_SCRIPTS)
    }

    fun runScript(scriptId: Int) {
        socket?.emit(SocketEvents.Emit.RUN_SCRIPT, scriptId)
    }

    fun addScript(title: String, code: String) {
        socket?.emit(
            SocketEvents.Emit.ADD_SCRIPT,
            JSONObject().apply {
                put("title", title)
                put("code", code)
            }
        )
    }

    fun modifyScript(scriptId: Int, title: String, code: String) {
        socket?.emit(
            SocketEvents.Emit.MODIFY_SCRIPT,
            JSONObject().apply {
                put("id", scriptId)
                put("title", title)
                put("code", code)
            }
        )
    }

    fun removeScript(scriptId: Int) {
        socket?.emit(
            SocketEvents.Emit.REMOVE_SCRIPT,
            JSONObject().apply {
                put("id", scriptId)
            }
        )
    }

    fun addScriptSchedule(scriptId: Int, schedule: String) {
        socket?.emit(
            SocketEvents.Emit.ADD_SCRIPT_SCHEDULE,
            JSONObject().apply {
                put("id", scriptId)
                put("schedule", schedule)
            }
        )
    }

    fun removeScriptSchedule(scriptId: Int) {
        socket?.emit(
            SocketEvents.Emit.REMOVE_SCRIPT_SCHEDULE,
            JSONObject().apply {
                put("id", scriptId)
            }
        )
    }

    companion object {
        private const val TAG = "SocketManager"
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
