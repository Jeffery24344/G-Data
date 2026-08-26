package com.gdata.app.domain.model

import java.time.LocalDate

data class BundleInfo(
    val totalBytes: Long = 0L,
    val remainingBytes: Long = 0L,
    val startDate: LocalDate? = null,
    val expiryDate: LocalDate? = null
) {
    val daysLeft: Int
        get() {
            val expiry = expiryDate ?: return 0
            val today = LocalDate.now()
            return if (expiry.isAfter(today) || expiry.isEqual(today)) {
                java.time.temporal.ChronoUnit.DAYS.between(today, expiry).toInt().coerceAtLeast(0)
            } else 0
        }

    val recommendedDailyBytes: Long
        get() = if (daysLeft > 0) remainingBytes / daysLeft else 0L

    val isValid: Boolean
        get() = totalBytes > 0 && remainingBytes >= 0
}
