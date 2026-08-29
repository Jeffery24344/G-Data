package com.gdata.app.ui.datasaver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gdata.app.domain.manager.ModeManager
import com.gdata.app.domain.model.OptimizationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataSaverUiState(
    val mode: OptimizationMode = OptimizationMode.BALANCED,
    val optimizationEnabled: Boolean = true,
    val gamingMode: Boolean = false
)

@HiltViewModel
class DataSaverViewModel @Inject constructor(
    private val modeManager: ModeManager
) : ViewModel() {

    val uiState: StateFlow<DataSaverUiState> = combine(
        modeManager.currentMode,
        modeManager.isOptimizationEnabled,
        modeManager.isGamingModeEnabled
    ) { mode, opt, gaming ->
        DataSaverUiState(mode, opt, gaming)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DataSaverUiState())

    fun setMode(mode: OptimizationMode) {
        viewModelScope.launch {
            if (mode != OptimizationMode.PERFORMANCE) {
                modeManager.setGamingMode(false)
            }
            modeManager.setMode(mode)
        }
    }

    fun setOptimizationEnabled(enabled: Boolean) {
        viewModelScope.launch { modeManager.setOptimizationEnabled(enabled) }
    }

    fun setGamingMode(enabled: Boolean) {
        viewModelScope.launch { modeManager.setGamingMode(enabled) }
    }
}
