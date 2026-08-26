package com.gdata.app.domain.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gdata.app.domain.model.OptimizationMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("gdata_prefs")

@Singleton
class ModeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val MODE_KEY = stringPreferencesKey("optimization_mode")
    private val OPTIMIZATION_ENABLED_KEY = stringPreferencesKey("optimization_enabled")
    private val GAMING_MODE_KEY = stringPreferencesKey("gaming_mode_enabled")

    val currentMode: Flow<OptimizationMode> = context.dataStore.data.map { prefs ->
        val name = prefs[MODE_KEY] ?: OptimizationMode.DEFAULT.name
        runCatching { OptimizationMode.valueOf(name) }.getOrDefault(OptimizationMode.DEFAULT)
    }

    val isOptimizationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[OPTIMIZATION_ENABLED_KEY]?.toBoolean() ?: true
    }

    val isGamingModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[GAMING_MODE_KEY]?.toBoolean() ?: false
    }

    suspend fun setMode(mode: OptimizationMode) {
        context.dataStore.edit { it[MODE_KEY] = mode.name }
    }

    suspend fun setOptimizationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[OPTIMIZATION_ENABLED_KEY] = enabled.toString() }
    }

    suspend fun setGamingMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[GAMING_MODE_KEY] = enabled.toString()
            if (enabled) {
                prefs[MODE_KEY] = OptimizationMode.PERFORMANCE.name
            }
        }
    }
}
