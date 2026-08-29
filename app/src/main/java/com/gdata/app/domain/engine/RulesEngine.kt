package com.gdata.app.domain.engine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.gdata.app.data.repository.NetworkStatsRepository
import com.gdata.app.domain.manager.BundleManager
import com.gdata.app.domain.manager.ModeManager
import com.gdata.app.domain.model.OptimizationMode
import com.gdata.app.notification.GDataNotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Automatically adjusts optimization mode based on battery, remaining data,
 * daily usage, and Wi-Fi vs mobile. Does not claim fake speed increases.
 */
@Singleton
class RulesEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modeManager: ModeManager,
    private val bundleManager: BundleManager,
    private val networkStatsRepository: NetworkStatsRepository,
    private val notifications: GDataNotificationHelper
) {

    suspend fun evaluate() {
        if (!modeManager.isOptimizationEnabled.first()) return
        if (modeManager.isGamingModeEnabled.first()) return // gaming locks Performance

        val battery = batteryPercent()
        val wifi = isWifi()
        val mobile = isMobile()
        val remaining = bundleManager.bundleInfo.first().remainingBytes
        val today = if (networkStatsRepository.hasPermission()) {
            networkStatsRepository.getTodayMobileUsage().totalBytes
        } else 0L

        val current = modeManager.currentMode.first()

        when {
            remaining in 1 until 1_000_000_000L && mobile -> {
                if (current != OptimizationMode.EXTREME) {
                    modeManager.setMode(OptimizationMode.EXTREME)
                    notifications.notifyUsage(
                        "Extreme Data Saver on",
                        "Remaining data is under 1 GB on mobile"
                    )
                }
            }
            battery <= 20 && mobile -> {
                if (current != OptimizationMode.EXTREME) {
                    modeManager.setMode(OptimizationMode.EXTREME)
                    notifications.notifyUsage(
                        "Extreme Data Saver on",
                        "Battery at $battery% on mobile data"
                    )
                }
            }
            today >= 500_000_000L && mobile -> {
                if (current != OptimizationMode.EXTREME) {
                    modeManager.setMode(OptimizationMode.EXTREME)
                    notifications.notifyUsage(
                        "High usage today",
                        "Over 500 MB mobile data used — switched to Extreme"
                    )
                }
            }
            wifi && current == OptimizationMode.EXTREME -> {
                modeManager.setMode(OptimizationMode.BALANCED)
                // Silent when relaxing on Wi-Fi
            }
        }
    }

    private fun batteryPercent(): Int {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val status = context.registerReceiver(null, filter) ?: return 100
        val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) (level * 100 / scale) else 100
    }

    private fun isWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isMobile(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
