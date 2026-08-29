package com.gdata.app.data.repository

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.NetworkTemplate
import android.os.Build
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

    private fun mobileTemplate(): NetworkTemplate {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            NetworkTemplate.Builder(NetworkTemplate.MATCH_MOBILE).build()
        } else {
            @Suppress("DEPRECATION")
            NetworkTemplate.buildTemplateMobileWildcard()
        }
    }

    suspend fun getTodayMobileUsage(): PeriodUsage = withContext(Dispatchers.IO) {
        queryPeriod(startOfToday(), System.currentTimeMillis())
    }

    suspend fun getMonthMobileUsage(): PeriodUsage = withContext(Dispatchers.IO) {
        queryPeriod(startOfMonth(), System.currentTimeMillis())
    }

    suspend fun getTopAppsThisMonth(limit: Int = 20): List<AppUsageRow> =
        withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext emptyList()
            val start = startOfMonth()
            val end = System.currentTimeMillis()
            val uidMap = mutableMapOf<Int, Long>()
            try {
                val stats = networkStatsManager.querySummary(mobileTemplate(), start, end)
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
            } catch (_: SecurityException) {
                return@withContext emptyList()
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
                AppUsageRow(
                    packageName = pkg,
                    appName = label,
                    totalBytes = bytes,
                    percentage = (bytes.toFloat() / total) * 100f
                )
            }.sortedByDescending { it.totalBytes }.take(limit)
        }

    private fun queryPeriod(start: Long, end: Long): PeriodUsage {
        if (!hasPermission()) return PeriodUsage(0, start, end)
        var rx = 0L
        var tx = 0L
        try {
            val stats = networkStatsManager.querySummary(mobileTemplate(), start, end)
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
}
