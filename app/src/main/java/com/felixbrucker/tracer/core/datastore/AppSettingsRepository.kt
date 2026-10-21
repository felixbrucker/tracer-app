package com.felixbrucker.tracer.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tracer_settings")

data class TracerSettings(
    val pingIntervalMs: Long = 1000L,
    val pingTimeoutSec: Int = 2,
    val maxHops: Int = 30,
    val lastUsedHost: String = ""
)

@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val PING_INTERVAL_MS = longPreferencesKey("ping_interval_ms")
        val PING_TIMEOUT_SEC = intPreferencesKey("ping_timeout_sec")
        val MAX_HOPS = intPreferencesKey("max_hops")
        val LAST_USED_HOST = stringPreferencesKey("last_used_host")
    }

    val tracerSettings: Flow<TracerSettings> = context.dataStore.data
        .map { preferences ->
            TracerSettings(
                pingIntervalMs = preferences[PreferencesKeys.PING_INTERVAL_MS] ?: 1000L,
                pingTimeoutSec = preferences[PreferencesKeys.PING_TIMEOUT_SEC] ?: 2,
                maxHops = preferences[PreferencesKeys.MAX_HOPS] ?: 30,
                lastUsedHost = preferences[PreferencesKeys.LAST_USED_HOST] ?: ""
            )
        }

    suspend fun updatePingIntervalMs(intervalMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PING_INTERVAL_MS] = intervalMs
        }
    }

    suspend fun updatePingTimeoutSec(timeoutSec: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PING_TIMEOUT_SEC] = timeoutSec
        }
    }

    suspend fun updateMaxHops(maxHops: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_HOPS] = maxHops
        }
    }

    suspend fun updateLastUsedHost(host: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_USED_HOST] = host
        }
    }
}
