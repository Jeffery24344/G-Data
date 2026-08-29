package com.gdata.app.ui.apps

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gdata.app.data.repository.AppUsageRow
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
    val apps: List<AppUsageRow> = emptyList(),
    val totalBytes: Long = 0L
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val has = networkStatsRepository.hasPermission()
            if (!has) {
                _uiState.value = AppsUiState(isLoading = false, hasPermission = false)
                return@launch
            }
            val apps = networkStatsRepository.getTopAppsThisMonth(30)
            _uiState.value = AppsUiState(
                isLoading = false,
                hasPermission = true,
                apps = apps,
                totalBytes = apps.sumOf { it.totalBytes }
            )
        }
    }

    fun openUsageAccess() {
        UsageAccessHelper.openUsageAccessSettings(context)
    }
}
