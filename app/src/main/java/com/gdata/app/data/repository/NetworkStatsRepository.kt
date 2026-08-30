package com.gdata.app.data.repository

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager
import com.gdata.app.util.UsageAccessHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class PeriodUsage(
    val totalBytes: Long,
    val mobileBytes: Long,
    val wifiBytes: Long,
    val startTime: Long,
    val endTime: Long
)

data class AppUsageRow(
    val packageName: String,
    val appName: String,
    val totalBytes: Long,
    val percentage: Float
)

data class DayUsageRow(
    val dayLabel: String,
    val dayStart: Long,
    val bytes: Long
)

enum class AppsPeriod { TODAY, WEEK, MONTH }

@Singleton
class NetworkStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val networkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    private val packageManager = context.packageManager

    fun hasPermission(): Boolean = UsageAccessHelper.hasUsageAccess(context)

    private fun startOfToday(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun startOfMonth(): Long =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun startOfWeek(): Long =
        Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis > System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, -1)
            }
        }.timeInMillis

    private fun daysAgo(days: Int): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun subscriberIds(): List<String?> {
        val ids = mutableListOf<String?>(null, "")
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                @Suppress("DEPRECATION")
                val sid = tm?.subscriberId
                if (!sid.isNullOrBlank()) ids.add(0, sid)
            }
        } catch (_: Exception) {
        }
        return ids.distinct()
    }

    private fun queryNetworkType(networkType: Int, start: Long, end: Long): Long {
        var total = 0L
        for (subscriber in subscriberIds()) {
            try {
                @Suppress("DEPRECATION")
                val stats = networkStatsManager.querySummary(networkType, subscriber, start, end)
                val bucket = NetworkStats.Bucket()
                var sum = 0L
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    sum += bucket.rxBytes + bucket.txBytes
                }
                stats.close()
                if (sum > total) total = sum
            } catch (_: Exception) {
            }
        }
        return total
    }

    private fun queryPeriod(start: Long, end: Long): PeriodUsage {
        if (!hasPermission()) return PeriodUsage(0, 0, 0, start, end)
        val mobile = queryNetworkType(ConnectivityManager.TYPE_MOBILE, start, end)
        val wifi = queryNetworkType(ConnectivityManager.TYPE_WIFI, start, end)
        return PeriodUsage(mobile, mobile, wifi, start, end)
    }

    suspend fun getTodayMobileUsage(): PeriodUsage = withContext(Dispatchers.IO) {
        queryPeriod(startOfToday(), System.currentTimeMillis())
    }

    suspend fun getWeekMobileUsage(): PeriodUsage = withContext(Dispatchers.IO) {
        queryPeriod(startOfWeek(), System.currentTimeMillis())
    }

    suspend fun getMonthMobileUsage(): PeriodUsage = withContext(Dispatchers.IO) {
        queryPeriod(startOfMonth(), System.currentTimeMillis())
    }

    suspend fun getLast7DaysBreakdown(): List<DayUsageRow> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val result = mutableListOf<DayUsageRow>()
        for (i in 6 downTo 0) {
            val dayEnd = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = dayEnd.timeInMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endMs = if (i == 0) System.currentTimeMillis() else dayEnd.timeInMillis
            val usage = queryPeriod(dayStart.timeInMillis, endMs)
            val label = android.text.format.DateFormat.format("EEE", dayStart).toString()
            result.add(DayUsageRow(label, dayStart.timeInMillis, usage.mobileBytes))
        }
        result
    }

    /**
     * Per-app usage. Tries mobile first; if empty, falls back to mobile+Wi-Fi
     * so recently used apps still appear when the user was on Wi-Fi.
     */
    suspend fun getTopApps(
        period: AppsPeriod = AppsPeriod.MONTH,
        limit: Int = 50,
        includeWifi: Boolean = true
    ): List<AppUsageRow> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()

        val end = System.currentTimeMillis()
        val start = when (period) {
            AppsPeriod.TODAY -> startOfToday()
            AppsPeriod.WEEK -> daysAgo(7)
            AppsPeriod.MONTH -> daysAgo(30)
        }

        var rows = collectApps(start, end, mobileOnly = true, limit = limit)
        if (rows.isEmpty() && includeWifi) {
            rows = collectApps(start, end, mobileOnly = false, limit = limit)
        }
        // Last resort: widen window to 90 days
        if (rows.isEmpty()) {
            rows = collectApps(daysAgo(90), end, mobileOnly = false, limit = limit)
        }
        rows
    }

    private fun collectApps(
        start: Long,
        end: Long,
        mobileOnly: Boolean,
        limit: Int
    ): List<AppUsageRow> {
        val uidMap = mutableMapOf<Int, Long>()
        val types = if (mobileOnly) {
            listOf(ConnectivityManager.TYPE_MOBILE)
        } else {
            listOf(ConnectivityManager.TYPE_MOBILE, ConnectivityManager.TYPE_WIFI)
        }

        for (type in types) {
            for (subscriber in subscriberIds()) {
                try {
                    @Suppress("DEPRECATION")
                    val stats = networkStatsManager.querySummary(type, subscriber, start, end)
                    val bucket = NetworkStats.Bucket()
                    while (stats.hasNextBucket()) {
                        stats.getNextBucket(bucket)
                        val uid = bucket.uid
                        if (uid < 0 ||
                            uid == NetworkStats.Bucket.UID_ALL ||
                            uid == NetworkStats.Bucket.UID_REMOVED ||
                            uid == NetworkStats.Bucket.UID_TETHERING
                        ) continue
                        val bytes = bucket.rxBytes + bucket.txBytes
                        if (bytes <= 0L) continue
                        uidMap[uid] = (uidMap[uid] ?: 0L) + bytes
                    }
                    stats.close()
                } catch (_: Exception) {
                }
            }
        }

        if (uidMap.isEmpty()) return emptyList()

        val total = uidMap.values.sum().coerceAtLeast(1L)
        return uidMap.mapNotNull { (uid, bytes) ->
            val packages = packageManager.getPackagesForUid(uid) ?: return@mapNotNull null
            // Prefer non-system launcher label when multiple packages share a UID
            val pkg = packages.maxByOrNull { p ->
                try {
                    val ai = packageManager.getApplicationInfo(p, 0)
                    if (ai.flags and ApplicationInfo.FLAG_SYSTEM == 0) 2 else 1
                } catch (_: Exception) {
                    0
                }
            } ?: packages.first()

            val label = try {
                val ai = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(ai).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                pkg
            }

            AppUsageRow(
                packageName = pkg,
                appName = label,
                totalBytes = bytes,
                percentage = (bytes.toFloat() / total) * 100f
            )
        }.sortedByDescending { it.totalBytes }.take(limit)
    }

    suspend fun getTopAppsThisMonth(limit: Int = 50): List<AppUsageRow> =
        getTopApps(AppsPeriod.MONTH, limit, includeWifi = true)

    fun startOfTodayMs(): Long = startOfToday()
    fun startOfWeekMs(): Long = startOfWeek()
    fun startOfMonthMs(): Long = startOfMonth()
}
