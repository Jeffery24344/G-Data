package com.gdata.app.ui.apps

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gdata.app.data.repository.AppUsageRow
import com.gdata.app.data.repository.AppsPeriod
import com.gdata.app.data.repository.NetworkStatsRepository
import com.gdata.app.util.UsageAccessHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppsUiState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val period: AppsPeriod = AppsPeriod.WEEK,
    val apps: List<AppUsageRow> = emptyList(),
    val totalBytes: Long = 0L,
    val emptyMessage: String = ""
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val networkStatsRepository: NetworkStatsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectPeriod(period: AppsPeriod) {
        _uiState.value = _uiState.value.copy(period = period)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val period = _uiState.value.period
            _uiState.value = _uiState.value.copy(isLoading = true)
            val has = networkStatsRepository.hasPermission()
            if (!has) {
                _uiState.value = AppsUiState(
                    isLoading = false,
                    hasPermission = false,
                    period = period
                )
                return@launch
            }
            val apps = networkStatsRepository.getTopApps(
                period = period,
                limit = 50,
                includeWifi = true
            )
            val empty = if (apps.isEmpty()) {
                "No app data for this period yet. Use mobile data or Wi‑Fi for a few minutes, then pull to refresh."
            } else ""
            _uiState.value = AppsUiState(
                isLoading = false,
                hasPermission = true,
                period = period,
                apps = apps,
                totalBytes = apps.sumOf { it.totalBytes },
                emptyMessage = empty
            )
        }
    }

    fun openUsageAccess() {
        UsageAccessHelper.openUsageAccessSettings(context)
    }
}
