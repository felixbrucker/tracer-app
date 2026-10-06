package com.felixbrucker.tracer.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.preferencesDataStore
import com.felixbrucker.tracer.core.model.TraceSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "tracer_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val MAX_HOPS = intPreferencesKey("max_hops")
        val PACKET_SIZE = intPreferencesKey("packet_size")
        val TIMEOUT_MS = intPreferencesKey("timeout_ms")
        val INTERVAL_MS = longPreferencesKey("interval_ms")
        val RESOLVE_DNS = booleanPreferencesKey("resolve_dns")
    }

    val settingsFlow: Flow<TraceSettings> = context.dataStore.data.map { prefs ->
        TraceSettings(
            maxHops = prefs[Keys.MAX_HOPS] ?: 30,
            packetSize = prefs[Keys.PACKET_SIZE] ?: 64,
            timeoutMs = prefs[Keys.TIMEOUT_MS] ?: 1000,
            intervalMs = prefs[Keys.INTERVAL_MS] ?: 1000L,
            resolveDns = prefs[Keys.RESOLVE_DNS] ?: true
        )
    }

    suspend fun updateSettings(settings: TraceSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_HOPS] = settings.maxHops
            prefs[Keys.PACKET_SIZE] = settings.packetSize
            prefs[Keys.TIMEOUT_MS] = settings.timeoutMs
            prefs[Keys.INTERVAL_MS] = settings.intervalMs
            prefs[Keys.RESOLVE_DNS] = settings.resolveDns
        }
    }
}
