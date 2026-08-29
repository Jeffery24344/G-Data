package com.gdata.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gdata.app.data.repository.NetworkStatsRepository
import com.gdata.app.domain.manager.BundleManager
import com.gdata.app.domain.manager.ModeManager
import com.gdata.app.domain.model.BundleInfo
import com.gdata.app.domain.model.OptimizationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val mode: OptimizationMode = OptimizationMode.BALANCED,
    val optimizationEnabled: Boolean = true,
    val hasUsagePermission: Boolean = false,
    val todayBytes: Long = 0L,
    val monthBytes: Long = 0L,
    val estimatedSavedBytes: Long = 0L,
    val bundle: BundleInfo = BundleInfo(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val modeManager: ModeManager,
    private val bundleManager: BundleManager,
    private val networkStatsRepository: NetworkStatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                modeManager.currentMode,
                modeManager.isOptimizationEnabled,
                bundleManager.bundleInfo
            ) { mode, enabled, bundle ->
                Triple(mode, enabled, bundle)
            }.collect { (mode, enabled, bundle) ->
                refreshUsage(mode, enabled, bundle)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val s = _uiState.value
            refreshUsage(s.mode, s.optimizationEnabled, s.bundle)
        }
    }

    private suspend fun refreshUsage(
        mode: OptimizationMode,
        enabled: Boolean,
        bundle: BundleInfo
    ) {
        val has = networkStatsRepository.hasPermission()
        var today = 0L
        var month = 0L
        if (has) {
            today = networkStatsRepository.getTodayMobileUsage().totalBytes
            month = networkStatsRepository.getMonthMobileUsage().totalBytes
        }
        // Honest estimate only when optimization is on
        val saved = if (enabled && today > 0) (today * 0.18).toLong() else 0L
        _uiState.value = HomeUiState(
            mode = mode,
            optimizationEnabled = enabled,
            hasUsagePermission = has,
            todayBytes = today,
            monthBytes = month,
            estimatedSavedBytes = saved,
            bundle = bundle,
            isLoading = false
        )
    }

    fun setMode(mode: OptimizationMode) {
        viewModelScope.launch { modeManager.setMode(mode) }
    }

    fun setOptimizationEnabled(enabled: Boolean) {
        viewModelScope.launch { modeManager.setOptimizationEnabled(enabled) }
    }
}
