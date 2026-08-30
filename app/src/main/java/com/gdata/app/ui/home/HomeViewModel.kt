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
    val todayWifiBytes: Long = 0L,
    val estimatedSavedBytes: Long = 0L,
    val policySummary: String = "",
    val dataSourceNote: String = "",
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
                arrayOf(mode, enabled, gaming, bundle)
            }.collect { arr ->
                @Suppress("UNCHECKED_CAST")
                refreshUsage(
                    arr[0] as OptimizationMode,
                    arr[1] as Boolean,
                    arr[2] as Boolean,
                    arr[3] as BundleInfo
                )
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
        var wifi = 0L
        if (has) {
            val t = networkStatsRepository.getTodayMobileUsage()
            val m = networkStatsRepository.getMonthMobileUsage()
            today = t.mobileBytes
            month = m.mobileBytes
            wifi = t.wifiBytes
        }
        val factor = ModePolicy.estimatedSavingsFactor(mode, enabled && !gaming)
        val saved = (today * factor).toLong()

        val note = when {
            !has -> "Grant Usage Access for real numbers"
            today == 0L && wifi == 0L -> "No data recorded yet for today (or stats not available on this device)"
            today == 0L && wifi > 0L -> "On Wi‑Fi today — mobile total is 0 (normal)"
            else -> "Today/Month = real mobile usage from Android. Savings = estimate only."
        }

        _uiState.value = HomeUiState(
            mode = mode,
            optimizationEnabled = enabled,
            gamingMode = gaming,
            hasUsagePermission = has,
            todayBytes = today,
            monthBytes = month,
            todayWifiBytes = wifi,
            estimatedSavedBytes = saved,
            policySummary = ModePolicy.activeSummary(mode, enabled, gaming),
            dataSourceNote = note,
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
