package com.homesoil.app.ui.screens.login

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesoil.app.data.repository.HomesoilRepository
import com.homesoil.app.network.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class LoginViewModel(
    private val repository: HomesoilRepository,
    private val context: Context
) : ViewModel() {

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token

    private val _serverHost = MutableStateFlow("")
    val serverHost: StateFlow<String> = _serverHost

    private val _serverPort = MutableStateFlow("4000")
    val serverPort: StateFlow<String> = _serverPort

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _discoveredServers = MutableStateFlow<List<String>>(emptyList())
    val discoveredServers: StateFlow<List<String>> = _discoveredServers

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

    fun scanNetwork() {
        if (_isScanning.value) return

        viewModelScope.launch {
            _isScanning.value = true
            _discoveredServers.value = emptyList()
            _error.value = null

            try {
                val localIp = getLocalIpAddress()
                if (localIp == null) {
                    _error.value = "Could not determine local IP address"
                    _isScanning.value = false
                    return@launch
                }

                val subnet = localIp.substringBeforeLast(".")
                val port = _serverPort.value.toIntOrNull() ?: 4000
                val found = mutableListOf<String>()

                withContext(Dispatchers.IO) {
                    // Scan subnet in batches
                    val results = (1..254).map { i ->
                        async {
                            val ip = "$subnet.$i"
                            try {
                                val socket = Socket()
                                socket.connect(InetSocketAddress(ip, port), 300)
                                socket.close()
                                ip
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }.awaitAll()

                    found.addAll(results.filterNotNull())
                }

                _discoveredServers.value = found
                if (found.isEmpty()) {
                    _error.value = "No Homesoil server found on the network"
                } else if (found.size == 1) {
                    _serverHost.value = found.first()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network scan failed", e)
                _error.value = "Network scan failed: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun selectServer(host: String) {
        _serverHost.value = host
        _discoveredServers.value = emptyList()
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get local IP", e)
        }
        return null
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

    companion object {
        private const val TAG = "LoginViewModel"
    }
}
