package com.homesoil.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesoil.app.data.repository.HomesoilRepository
import com.homesoil.app.network.ConnectionState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: HomesoilRepository
) : ViewModel() {

    private val _serverHost = MutableStateFlow("")
    val serverHost: StateFlow<String> = _serverHost

    private val _serverPort = MutableStateFlow("4000")
    val serverPort: StateFlow<String> = _serverPort

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting

    private val _testResult = MutableStateFlow<TestResult?>(null)
    val testResult: StateFlow<TestResult?> = _testResult

    val connectionState = repository.connectionState

    init {
        viewModelScope.launch {
            repository.serverHost.collect { host ->
                _serverHost.value = host
            }
        }
        viewModelScope.launch {
            repository.serverPort.collect { port ->
                _serverPort.value = port.toString()
            }
        }
        viewModelScope.launch {
            repository.authToken.collect { savedToken ->
                _token.value = savedToken
            }
        }
    }

    fun updateServerHost(host: String) {
        _serverHost.value = host
        _testResult.value = null
    }

    fun updateServerPort(port: String) {
        _serverPort.value = port
        _testResult.value = null
    }

    fun updateToken(newToken: String) {
        _token.value = newToken
        _testResult.value = null
    }

    fun testConnection() {
        val host = _serverHost.value.trim()
        val port = _serverPort.value.toIntOrNull() ?: 4000
        val tokenValue = _token.value.trim()

        if (host.isEmpty()) {
            _testResult.value = TestResult.Error("Server address is required")
            return
        }

        if (tokenValue.isEmpty()) {
            _testResult.value = TestResult.Error("Token is required")
            return
        }

        _isTesting.value = true
        _testResult.value = null

        viewModelScope.launch {
            // Disconnect first if connected
            repository.disconnect()

            // Connect with new settings
            val serverUrl = "http://$host:$port"
            repository.connect(serverUrl, tokenValue)

            // Wait for connection result
            connectionState.first { state ->
                when (state) {
                    ConnectionState.CONNECTED -> {
                        _testResult.value = TestResult.Success
                        _isTesting.value = false
                        true
                    }
                    ConnectionState.ERROR -> {
                        _testResult.value = TestResult.Error("Connection failed")
                        _isTesting.value = false
                        true
                    }
                    else -> false
                }
            }
        }
    }

    fun saveAndReconnect() {
        val host = _serverHost.value.trim()
        val port = _serverPort.value.toIntOrNull() ?: 4000
        val tokenValue = _token.value.trim()

        viewModelScope.launch {
            repository.saveServerSettings(host, port)
            repository.saveAuthToken(tokenValue)
        }

        repository.disconnect()
        val serverUrl = "http://$host:$port"
        repository.connect(serverUrl, tokenValue)
    }

    fun disconnect() {
        repository.disconnect()
    }
}

sealed class TestResult {
    data object Success : TestResult()
    data class Error(val message: String) : TestResult()
}
