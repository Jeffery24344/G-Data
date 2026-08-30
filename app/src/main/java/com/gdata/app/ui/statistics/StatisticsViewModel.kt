package com.gdata.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gdata.app.data.repository.AppUsageRow
import com.gdata.app.data.repository.AppsPeriod
import com.gdata.app.data.repository.DayUsageRow
import com.gdata.app.data.repository.NetworkStatsRepository
import com.gdata.app.domain.manager.ModeManager
import com.gdata.app.domain.model.ModePolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatsPeriod { TODAY, WEEK, MONTH }

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val period: StatsPeriod = StatsPeriod.WEEK,
    val totalBytes: Long = 0L,
    val estimatedSavedBytes: Long = 0L,
    val daily: List<DayUsageRow> = emptyList(),
    val topApps: List<AppUsageRow> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val networkStatsRepository: NetworkStatsRepository,
    private val modeManager: ModeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        load(StatsPeriod.WEEK)
    }

    fun selectPeriod(period: StatsPeriod) {
        load(period)
    }

    fun refresh() {
        load(_uiState.value.period)
    }

    private fun load(period: StatsPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, period = period)
            val has = networkStatsRepository.hasPermission()
            if (!has) {
                _uiState.value = StatisticsUiState(
                    isLoading = false,
                    hasPermission = false,
                    period = period
                )
                return@launch
            }

            val usage = when (period) {
                StatsPeriod.TODAY -> networkStatsRepository.getTodayMobileUsage()
                StatsPeriod.WEEK -> networkStatsRepository.getWeekMobileUsage()
                StatsPeriod.MONTH -> networkStatsRepository.getMonthMobileUsage()
            }

            val appsPeriod = when (period) {
                StatsPeriod.TODAY -> AppsPeriod.TODAY
                StatsPeriod.WEEK -> AppsPeriod.WEEK
                StatsPeriod.MONTH -> AppsPeriod.MONTH
            }

            val top = networkStatsRepository.getTopApps(
                period = appsPeriod,
                limit = 5,
                includeWifi = true
            )
            val daily = if (period == StatsPeriod.WEEK || period == StatsPeriod.TODAY) {
                networkStatsRepository.getLast7DaysBreakdown()
            } else {
                emptyList()
            }

            val mode = modeManager.currentMode.first()
            val optOn = modeManager.isOptimizationEnabled.first()
            val gaming = modeManager.isGamingModeEnabled.first()
            val factor = ModePolicy.estimatedSavingsFactor(mode, optOn && !gaming)
            val saved = (usage.totalBytes * factor).toLong()

            _uiState.value = StatisticsUiState(
                isLoading = false,
                hasPermission = true,
                period = period,
                totalBytes = usage.totalBytes,
                estimatedSavedBytes = saved,
                daily = daily,
                topApps = top
            )
        }
    }
}
