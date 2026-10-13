package com.felixbrucker.tracer.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felixbrucker.tracer.core.data.FavoriteRepository
import com.felixbrucker.tracer.core.data.TraceRepository
import com.felixbrucker.tracer.core.database.entity.FavoriteTargetEntity
import com.felixbrucker.tracer.core.datastore.AppSettingsRepository
import com.felixbrucker.tracer.core.datastore.TracerSettings
import com.felixbrucker.tracer.core.model.HopResult
import com.felixbrucker.tracer.core.model.MtrHopStats
import com.felixbrucker.tracer.core.model.PingResult
import com.felixbrucker.tracer.core.model.TraceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    traceRepository: TraceRepository,
    private val favoriteRepository: FavoriteRepository,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteTargetEntity>> = favoriteRepository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tracerSettings: StateFlow<TracerSettings> = appSettingsRepository.tracerSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TracerSettings())

    val pingResults: StateFlow<List<PingResult>> = traceRepository.pingResults
    val tracerouteHops: StateFlow<List<HopResult>> = traceRepository.tracerouteHops
    val winMtrStats: StateFlow<List<MtrHopStats>> = traceRepository.winMtrStats

    val activeTraceType: StateFlow<TraceType?> = traceRepository.activeTraceType
    val activeTarget: StateFlow<String?> = traceRepository.activeTarget

    fun addFavorite(title: String?, host: String) {
        viewModelScope.launch {
            favoriteRepository.addFavorite(title, host)
        }
    }

    fun updateFavorite(id: Long, title: String?, host: String) {
        viewModelScope.launch {
            favoriteRepository.updateFavorite(id, title, host)
        }
    }

    fun removeFavorite(id: Long) {
        viewModelScope.launch {
            favoriteRepository.removeFavorite(id)
        }
    }

    fun updatePingIntervalMs(intervalMs: Long) {
        viewModelScope.launch {
            appSettingsRepository.updatePingIntervalMs(intervalMs)
        }
    }

    fun updatePingTimeoutSec(timeoutSec: Int) {
        viewModelScope.launch {
            appSettingsRepository.updatePingTimeoutSec(timeoutSec)
        }
    }

    fun updateMaxHops(maxHops: Int) {
        viewModelScope.launch {
            appSettingsRepository.updateMaxHops(maxHops)
        }
    }
}
