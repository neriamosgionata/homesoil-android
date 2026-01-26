package com.homesoil.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesoil.app.data.repository.HomesoilRepository
import com.homesoil.app.network.ConnectionState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: HomesoilRepository
) : ViewModel() {

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token

    private val _serverHost = MutableStateFlow("")
    val serverHost: StateFlow<String> = _serverHost

    private val _serverPort = MutableStateFlow("4000")
    val serverPort: StateFlow<String> = _serverPort

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val connectionState = repository.connectionState
    val isConnecting = connectionState.map { it == ConnectionState.CONNECTING }
    val isConnected = connectionState.map { it == ConnectionState.CONNECTED }

    init {
        viewModelScope.launch {
            repository.authToken.collect { savedToken ->
                if (_token.value.isEmpty()) {
                    _token.value = savedToken
                }
            }
        }
        viewModelScope.launch {
            repository.serverHost.collect { host ->
                if (_serverHost.value.isEmpty()) {
                    _serverHost.value = host
                }
            }
        }
        viewModelScope.launch {
            repository.serverPort.collect { port ->
                if (_serverPort.value == "4000") {
                    _serverPort.value = port.toString()
                }
            }
        }
        viewModelScope.launch {
            repository.connectionError.collect { errorMsg ->
                _error.value = errorMsg
            }
        }
    }

    fun updateToken(newToken: String) {
        _token.value = newToken
        _error.value = null
    }

    fun updateServerHost(host: String) {
        _serverHost.value = host
        _error.value = null
    }

    fun updateServerPort(port: String) {
        _serverPort.value = port
        _error.value = null
    }

    fun connect() {
        val host = _serverHost.value.trim()
        val port = _serverPort.value.toIntOrNull() ?: 4000
        val tokenValue = _token.value.trim()

        if (host.isEmpty()) {
            _error.value = "Server address is required"
            return
        }

        if (tokenValue.isEmpty()) {
            _error.value = "Token is required"
            return
        }

        viewModelScope.launch {
            repository.saveServerSettings(host, port)
            repository.saveAuthToken(tokenValue)
        }

        val serverUrl = "http://$host:$port"
        repository.connect(serverUrl, tokenValue)
    }

    fun clearError() {
        _error.value = null
    }
}
