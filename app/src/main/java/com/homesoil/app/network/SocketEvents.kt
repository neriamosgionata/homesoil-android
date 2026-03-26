package com.homesoil.app.network

object SocketEvents {
    // Emit events (App -> Backend)
    object Emit {
        // Sensor events
        const val GET_SENSOR_READINGS = "get-sensor-readings"
        const val RENAME_SENSOR = "rename-sensor"
        const val REMOVE_SENSOR = "remove-sensor"

        // Actuator events
        const val TOGGLE_ACTUATOR = "toggle-actuator"
        const val PULSE_ACTUATOR = "pulse-actuator"
        const val INTERMITTENT_ACTUATOR = "intermittent-actuator"
        const val STOP_INTERMITTENT_ACTUATOR = "stop-intermittent-actuator"
        const val RENAME_ACTUATOR = "rename-actuator"
        const val REMOVE_ACTUATOR = "remove-actuator"

        // Script events
        const val RUN_SCRIPT = "run-script"
        const val GET_ALL_SCRIPTS = "get-all-scripts"
        const val ADD_SCRIPT = "add-script"
        const val REMOVE_SCRIPT = "remove-script"
        const val MODIFY_SCRIPT = "modify-script"
        const val ADD_SCRIPT_SCHEDULE = "add-script-schedule"
        const val REMOVE_SCRIPT_SCHEDULE = "remove-script-schedule"

        // Flow events
        const val ADD_FLOW = "add-flow"
        const val MODIFY_FLOW = "modify-flow"
        const val REMOVE_FLOW = "remove-flow"
        const val TOGGLE_FLOW = "toggle-flow"
    }

    // Listen events (Backend -> App)
    object Listen {
        // Generic messages
        const val MESSAGE_SENT = "message-sent"

        // Sensor events
        const val ALL_SENSORS = "all-sensors"
        const val ALL_LAST_SENSOR_READINGS = "all-last-sensors-reads"
        const val SENSOR_REGISTER = "sensor-register"
        const val SENSOR_UNREGISTER = "sensor-unregister"
        const val SENSOR_NAME_CHANGE = "sensor-name-change"
        const val SENSOR_READ = "sensor-read"
        const val ALL_SENSOR_READINGS = "all-sensor-reads"
        const val SENSOR_CHANGE_ONLINE = "sensor-change-online"

        // Actuator events
        const val ALL_ACTUATORS = "all-actuators"
        const val ACTUATOR_REGISTER = "actuator-register"
        const val ACTUATOR_UNREGISTER = "actuator-unregister"
        const val ACTUATOR_NAME_CHANGE = "actuator-name-change"
        const val ACTUATOR_STATE_CHANGE = "actuator-state-change"
        const val ACTUATOR_INTERMITTENT_CHANGE = "actuator-intermittent-change"
        const val ACTUATOR_CHANGE_ONLINE = "actuator-change-online"

        // Script events
        const val ALL_SCRIPTS = "all-scripts"
        const val SCRIPT_SAVED = "script-saved"
        const val SCRIPT_DELETED = "script-deleted"
        const val SCRIPT_MODIFIED = "script-modified"
        const val SCRIPT_STATUS_CHANGE = "script-status-change"
        const val SCRIPT_SCHEDULE_ADDED = "script-schedule-added"
        const val SCRIPT_SCHEDULE_REMOVED = "script-schedule-removed"

        // Flow events
        const val ALL_FLOWS = "all-flows"
        const val FLOW_SAVED = "flow-saved"
        const val FLOW_MODIFIED = "flow-modified"
        const val FLOW_DELETED = "flow-deleted"
        const val FLOW_TOGGLED = "flow-toggled"
    }
}
