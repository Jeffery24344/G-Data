package com.gdata.app.domain.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gdata.app.domain.model.BundleInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.bundleDataStore by preferencesDataStore("gdata_bundle")

@Singleton
class BundleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TOTAL_KEY = longPreferencesKey("bundle_total")
    private val REMAINING_KEY = longPreferencesKey("bundle_remaining")
    private val START_KEY = stringPreferencesKey("bundle_start")
    private val EXPIRY_KEY = stringPreferencesKey("bundle_expiry")

    val bundleInfo: Flow<BundleInfo> = context.bundleDataStore.data.map { prefs ->
        BundleInfo(
            totalBytes = prefs[TOTAL_KEY] ?: 0L,
            remainingBytes = prefs[REMAINING_KEY] ?: 0L,
            startDate = prefs[START_KEY]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            expiryDate = prefs[EXPIRY_KEY]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        )
    }

    suspend fun saveBundle(
        totalBytes: Long,
        remainingBytes: Long,
        daysLeft: Int
    ) {
        val expiry = if (daysLeft > 0) LocalDate.now().plusDays(daysLeft.toLong()) else null
        context.bundleDataStore.edit { prefs ->
            prefs[TOTAL_KEY] = totalBytes
            prefs[REMAINING_KEY] = remainingBytes
            prefs[START_KEY] = LocalDate.now().toString()
            if (expiry != null) prefs[EXPIRY_KEY] = expiry.toString()
            else prefs.remove(EXPIRY_KEY)
        }
    }
}
