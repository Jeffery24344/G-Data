package com.gdata.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gdata.app.domain.manager.BundleManager
import com.gdata.app.domain.model.BundleInfo
import com.gdata.app.notification.GDataNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val bundleManager: BundleManager,
    private val notificationHelper: GDataNotificationHelper
) : ViewModel() {

    val bundle: StateFlow<BundleInfo> = bundleManager.bundleInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BundleInfo())

    fun saveBundle(totalGb: String, remainingGb: String, daysLeft: String) {
        viewModelScope.launch {
            val total = ((totalGb.toDoubleOrNull() ?: 0.0) * 1_000_000_000L).toLong()
            val remaining = ((remainingGb.toDoubleOrNull() ?: 0.0) * 1_000_000_000L).toLong()
            val days = daysLeft.toIntOrNull() ?: 0
            bundleManager.saveBundle(total, remaining, days)
            if (days in 1..3) {
                notificationHelper.notifyBundle(
                    "Bundle reminder",
                    "$days day(s) left on your data package"
                )
            }
        }
    }

    fun testNotification() {
        notificationHelper.notifyUsage(
            "G Data",
            "Notifications are working. You will get alerts for high usage and bundle expiry."
        )
    }
}
