package com.gdata.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gdata.app.data.repository.NetworkStatsRepository
import com.gdata.app.domain.engine.RulesEngine
import com.gdata.app.domain.manager.BundleManager
import com.gdata.app.domain.manager.ModeManager
import com.gdata.app.domain.model.BundleInfo
import com.gdata.app.domain.model.ModePolicy
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
    val gamingMode: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val todayBytes: Long = 0L,
    val monthBytes: Long = 0L,
    val estimatedSavedBytes: Long = 0L,
    val policySummary: String = "",
    val bundle: BundleInfo = BundleInfo(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val modeManager: ModeManager,
    private val bundleManager: BundleManager,
    private val networkStatsRepository: NetworkStatsRepository,
    private val rulesEngine: RulesEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                modeManager.currentMode,
                modeManager.isOptimizationEnabled,
                modeManager.isGamingModeEnabled,
                bundleManager.bundleInfo
            ) { mode, enabled, gaming, bundle ->
                Quad(mode, enabled, gaming, bundle)
            }.collect { (mode, enabled, gaming, bundle) ->
                refreshUsage(mode, enabled, gaming, bundle)
            }
        }
        viewModelScope.launch {
            runCatching { rulesEngine.evaluate() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val s = _uiState.value
            refreshUsage(s.mode, s.optimizationEnabled, s.gamingMode, s.bundle)
            runCatching { rulesEngine.evaluate() }
        }
    }

    private suspend fun refreshUsage(
        mode: OptimizationMode,
        enabled: Boolean,
        gaming: Boolean,
        bundle: BundleInfo
    ) {
        val has = networkStatsRepository.hasPermission()
        var today = 0L
        var month = 0L
        if (has) {
            today = networkStatsRepository.getTodayMobileUsage().totalBytes
            month = networkStatsRepository.getMonthMobileUsage().totalBytes
        }
        val factor = ModePolicy.estimatedSavingsFactor(mode, enabled && !gaming)
        val saved = (today * factor).toLong()
        _uiState.value = HomeUiState(
            mode = mode,
            optimizationEnabled = enabled,
            gamingMode = gaming,
            hasUsagePermission = has,
            todayBytes = today,
            monthBytes = month,
            estimatedSavedBytes = saved,
            policySummary = ModePolicy.activeSummary(mode, enabled, gaming),
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

    private data class Quad<
        A, B, C, D
        >(
        val a: A, val b: B, val c: C, val d: D
    )
}
