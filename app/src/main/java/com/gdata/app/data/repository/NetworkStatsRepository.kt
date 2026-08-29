package com.gdata.app.data.repository

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import com.gdata.app.util.UsageAccessHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class PeriodUsage(
    val totalBytes: Long,
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
            result.add(DayUsageRow(label, dayStart.timeInMillis, usage.totalBytes))
        }
        result
    }

    suspend fun getTopApps(
        start: Long,
        end: Long,
        limit: Int = 10
    ): List<AppUsageRow> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val uidMap = mutableMapOf<Int, Long>()
        try {
            @Suppress("DEPRECATION")
            val stats = networkStatsManager.querySummary(
                ConnectivityManager.TYPE_MOBILE,
                null,
                start,
                end
            )
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val uid = bucket.uid
                if (uid == NetworkStats.Bucket.UID_ALL ||
                    uid == NetworkStats.Bucket.UID_REMOVED ||
                    uid == NetworkStats.Bucket.UID_TETHERING
                ) continue
                uidMap[uid] = (uidMap[uid] ?: 0L) + bucket.rxBytes + bucket.txBytes
            }
            stats.close()
        } catch (_: Exception) {
            return@withContext emptyList()
        }

        val total = uidMap.values.sum().coerceAtLeast(1L)
        uidMap.mapNotNull { (uid, bytes) ->
            val pkg = packageManager.getPackagesForUid(uid)?.firstOrNull() ?: return@mapNotNull null
            val label = try {
                val ai = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(ai).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                pkg
            }
            AppUsageRow(pkg, label, bytes, (bytes.toFloat() / total) * 100f)
        }.sortedByDescending { it.totalBytes }.take(limit)
    }

    suspend fun getTopAppsThisMonth(limit: Int = 20): List<AppUsageRow> =
        getTopApps(startOfMonth(), System.currentTimeMillis(), limit)

    private fun queryPeriod(start: Long, end: Long): PeriodUsage {
        if (!hasPermission()) return PeriodUsage(0, start, end)
        var rx = 0L
        var tx = 0L
        try {
            @Suppress("DEPRECATION")
            val stats = networkStatsManager.querySummary(
                ConnectivityManager.TYPE_MOBILE,
                null,
                start,
                end
            )
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                rx += bucket.rxBytes
                tx += bucket.txBytes
            }
            stats.close()
        } catch (_: Exception) {
            // ignore
        }
        return PeriodUsage(rx + tx, start, end)
    }

    fun startOfTodayMs(): Long = startOfToday()
    fun startOfWeekMs(): Long = startOfWeek()
    fun startOfMonthMs(): Long = startOfMonth()
}
