package com.gdata.app.ui.datasaver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gdata.app.domain.manager.ModeManager
import com.gdata.app.domain.model.ModePolicy
import com.gdata.app.domain.model.OptimizationMode
import com.gdata.app.notification.GDataNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataSaverUiState(
    val mode: OptimizationMode = OptimizationMode.BALANCED,
    val optimizationEnabled: Boolean = true,
    val gamingMode: Boolean = false,
    val statusLine: String = ""
)

@HiltViewModel
class DataSaverViewModel @Inject constructor(
    private val modeManager: ModeManager,
    private val notifications: GDataNotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataSaverUiState())
    val uiState: StateFlow<DataSaverUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                modeManager.currentMode,
                modeManager.isOptimizationEnabled,
                modeManager.isGamingModeEnabled
            ) { mode, enabled, gaming ->
                Triple(mode, enabled, gaming)
            }.collect { (mode, enabled, gaming) ->
                _uiState.value = DataSaverUiState(
                    mode = mode,
                    optimizationEnabled = enabled,
                    gamingMode = gaming,
                    statusLine = ModePolicy.activeSummary(mode, enabled, gaming)
                )
            }
        }
    }

    fun setMode(mode: OptimizationMode) {
        viewModelScope.launch {
            modeManager.setMode(mode)
            val msg = "${mode.displayName} applied — ${shortHint(mode)}"
            _events.emit(msg)
            notifications.notifyUsage("Mode: ${mode.displayName}", shortHint(mode))
        }
    }

    fun setOptimizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            modeManager.setOptimizationEnabled(enabled)
            val msg = if (enabled) "Optimization turned ON" else "Optimization paused"
            _events.emit(msg)
            notifications.notifyUsage(msg, "G Data policy updated")
        }
    }

    fun setGamingMode(enabled: Boolean) {
        viewModelScope.launch {
            modeManager.setGamingMode(enabled)
            val msg = if (enabled) {
                "Gaming Mode ON — Performance priority"
            } else {
                "Gaming Mode OFF"
            }
            _events.emit(msg)
            notifications.notifyUsage(msg, "G Data policy updated")
        }
    }

    private fun shortHint(mode: OptimizationMode): String = when (mode) {
        OptimizationMode.PERFORMANCE -> "Light policy, best responsiveness"
        OptimizationMode.BALANCED -> "Everyday balance of savings and speed"
        OptimizationMode.EXTREME -> "Strongest savings posture + system Data Saver tip"
    }
}
