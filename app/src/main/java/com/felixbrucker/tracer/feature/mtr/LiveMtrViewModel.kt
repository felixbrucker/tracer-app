package com.felixbrucker.tracer.feature.mtr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felixbrucker.tracer.core.data.FavoriteRepository
import com.felixbrucker.tracer.core.data.NetworkToolRepository
import com.felixbrucker.tracer.core.datastore.SettingsRepository
import com.felixbrucker.tracer.core.model.FavoriteTarget
import com.felixbrucker.tracer.core.model.HopStats
import com.felixbrucker.tracer.core.model.TraceMode
import com.felixbrucker.tracer.core.model.TraceSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveMtrUiState(
    val targetHost: String = "",
    val isTracing: Boolean = false,
    val isPaused: Boolean = false,
    val hops: List<HopStats> = emptyList(),
    val settings: TraceSettings = TraceSettings(),
    val isFavorite: Boolean = false
)

@HiltViewModel
class LiveMtrViewModel @Inject constructor(
    private val networkToolRepository: NetworkToolRepository,
    private val settingsRepository: SettingsRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveMtrUiState())
    val uiState: StateFlow<LiveMtrUiState> = _uiState.asStateFlow()

    private var mtrJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            _uiState.update { it.copy(settings = settings) }
        }
        viewModelScope.launch {
            favoriteRepository.favorites.collect { favorites ->
                val currentHost = _uiState.value.targetHost
                val isFav = favorites.any { it.host.equals(currentHost, ignoreCase = true) }
                _uiState.update { it.copy(isFavorite = isFav) }
            }
        }
    }

    fun onHostChanged(host: String) {
        _uiState.update { it.copy(targetHost = host) }
        checkIsFavorite(host)
    }

    private fun checkIsFavorite(host: String) {
        viewModelScope.launch {
            val favorites = favoriteRepository.favorites.first()
            val isFav = favorites.any { it.host.equals(host, ignoreCase = true) }
            _uiState.update { it.copy(isFavorite = isFav) }
        }
    }

    fun startTrace() {
        val host = _uiState.value.targetHost.trim()
        if (host.isEmpty()) return

        mtrJob?.cancel()
        _uiState.update {
            it.copy(
                isTracing = true,
                isPaused = false,
                hops = emptyList()
            )
        }

        mtrJob = viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val maxHops = settings.maxHops
            val hopStatsMap = mutableMapOf<Int, HopStats>()

            while (_uiState.value.isTracing) {
                if (_uiState.value.isPaused) {
                    delay(500)
                    continue
                }

                for (ttl in 1..maxHops) {
                    if (!_uiState.value.isTracing || _uiState.value.isPaused) break

                    val probe = networkToolRepository.probeHop(host, ttl, settings)
                    val existing = hopStatsMap[ttl] ?: HopStats(
                        hopNumber = ttl,
                        host = probe.host,
                        ip = probe.ip
                    )

                    val newSent = existing.sent + 1
                    val newReceived = if (probe.rttMs != null) existing.received + 1 else existing.received
                    val newLast = probe.rttMs
                    val newBest = if (probe.rttMs != null) {
                        minOf(existing.bestRttMs ?: Float.MAX_VALUE, probe.rttMs)
                    } else existing.bestRttMs
                    val newWorst = if (probe.rttMs != null) {
                        maxOf(existing.worstRttMs ?: 0f, probe.rttMs)
                    } else existing.worstRttMs
                    val newSum = if (probe.rttMs != null) existing.sumRttMs + probe.rttMs else existing.sumRttMs
                    val newSumSq = if (probe.rttMs != null) existing.sumSqRttMs + (probe.rttMs * probe.rttMs) else existing.sumSqRttMs
                    val newHistory = (existing.historyRttMs + probe.rttMs).takeLast(10)

                    val updatedHop = existing.copy(
                        host = if (probe.host != "*") probe.host else existing.host,
                        ip = probe.ip ?: existing.ip,
                        sent = newSent,
                        received = newReceived,
                        lastRttMs = newLast,
                        bestRttMs = newBest,
                        worstRttMs = newWorst,
                        sumRttMs = newSum,
                        sumSqRttMs = newSumSq,
                        historyRttMs = newHistory
                    )

                    hopStatsMap[ttl] = updatedHop
                    _uiState.update { currentState ->
                        currentState.copy(hops = hopStatsMap.values.sortedBy { it.hopNumber })
                    }

                    if (probe.isReached) break
                }
                delay(settings.intervalMs)
            }
        }
    }

    fun pauseTrace() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeTrace() {
        _uiState.update { it.copy(isPaused = false) }
    }

    fun stopTrace() {
        mtrJob?.cancel()
        _uiState.update { it.copy(isTracing = false, isPaused = false) }
    }

    fun resetStats() {
        _uiState.update { it.copy(hops = emptyList()) }
    }

    fun toggleFavorite() {
        val host = _uiState.value.targetHost.trim()
        if (host.isEmpty()) return

        viewModelScope.launch {
            val favorites = favoriteRepository.favorites.first()
            val existing = favorites.find { it.host.equals(host, ignoreCase = true) }
            if (existing != null) {
                favoriteRepository.removeFavorite(existing)
                _uiState.update { it.copy(isFavorite = false) }
            } else {
                favoriteRepository.addFavorite(
                    FavoriteTarget(
                        name = host,
                        host = host,
                        defaultMode = TraceMode.LIVE_MTR
                    )
                )
                _uiState.update { it.copy(isFavorite = true) }
            }
        }
    }
}
