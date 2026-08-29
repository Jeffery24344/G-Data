package com.gdata.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gdata.app.data.repository.AppUsageRow
import com.gdata.app.data.repository.DayUsageRow
import com.gdata.app.data.repository.NetworkStatsRepository
import com.gdata.app.domain.manager.ModeManager
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

            val now = System.currentTimeMillis()
            val (usage, start) = when (period) {
                StatsPeriod.TODAY ->
                    networkStatsRepository.getTodayMobileUsage() to networkStatsRepository.startOfTodayMs()
                StatsPeriod.WEEK ->
                    networkStatsRepository.getWeekMobileUsage() to networkStatsRepository.startOfWeekMs()
                StatsPeriod.MONTH ->
                    networkStatsRepository.getMonthMobileUsage() to networkStatsRepository.startOfMonthMs()
            }

            val top = networkStatsRepository.getTopApps(start, now, limit = 5)
            val daily = if (period == StatsPeriod.WEEK || period == StatsPeriod.TODAY) {
                networkStatsRepository.getLast7DaysBreakdown()
            } else {
                emptyList()
            }

            val optOn = modeManager.isOptimizationEnabled.first()
            val saved = if (optOn && usage.totalBytes > 0) {
                (usage.totalBytes * 0.18).toLong()
            } else 0L

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
