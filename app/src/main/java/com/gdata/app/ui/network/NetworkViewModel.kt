package com.gdata.app.ui.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gdata.app.domain.manager.ModeManager
import com.gdata.app.domain.model.OptimizationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

data class NetworkUiState(
    val connectionType: String = "Unknown",
    val operatorName: String = "—",
    val isMetered: Boolean = false,
    val modeName: String = OptimizationMode.BALANCED.displayName,
    val optimizationEnabled: Boolean = true,
    val latencyMs: Int? = null,
    val isTesting: Boolean = false,
    val testError: String? = null
)

@HiltViewModel
class NetworkViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modeManager: ModeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            modeManager.currentMode.collect { mode ->
                _uiState.update { it.copy(modeName = mode.displayName) }
            }
        }
        viewModelScope.launch {
            modeManager.isOptimizationEnabled.collect { enabled ->
                _uiState.update { it.copy(optimizationEnabled = enabled) }
            }
        }
    }

    fun refresh() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }

        val type = when {
            caps == null -> "None"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }

        val operator = try {
            tm?.networkOperatorName?.takeIf { it.isNotBlank() } ?: "—"
        } catch (_: SecurityException) {
            "—"
        }

        val metered = try {
            cm.isActiveNetworkMetered
        } catch (_: Exception) {
            type == "Mobile"
        }

        _uiState.update {
            it.copy(
                connectionType = type,
                operatorName = operator,
                isMetered = metered,
                testError = null
            )
        }
    }

    fun runLatencyTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testError = null) }
            val result = withContext(Dispatchers.IO) {
                measureLatencyMs()
            }
            _uiState.update {
                it.copy(
                    isTesting = false,
                    latencyMs = result.getOrNull(),
                    testError = result.exceptionOrNull()?.message
                )
            }
        }
    }

    /**
     * Real TCP connect time to a public DNS host (not a random demo number).
     * Uses port 53 / 443 with short timeout. Does not run continuously.
     */
    private fun measureLatencyMs(): Result<Int> {
        val hosts = listOf(
            "1.1.1.1" to 443,
            "8.8.8.8" to 443,
            "dns.google" to 443
        )
        var lastError: Exception? = null
        for ((host, port) in hosts) {
            try {
                val start = System.nanoTime()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 3000)
                }
                val ms = ((System.nanoTime() - start) / 1_000_000L).toInt()
                return Result.success(ms.coerceAtLeast(1))
            } catch (e: Exception) {
                lastError = e
            }
        }
        return Result.failure(lastError ?: Exception("Network unreachable"))
    }
}
