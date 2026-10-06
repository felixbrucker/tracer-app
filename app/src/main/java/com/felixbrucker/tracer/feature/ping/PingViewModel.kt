package com.felixbrucker.tracer.feature.ping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felixbrucker.tracer.core.data.FavoriteRepository
import com.felixbrucker.tracer.core.data.NetworkToolRepository
import com.felixbrucker.tracer.core.datastore.SettingsRepository
import com.felixbrucker.tracer.core.model.FavoriteTarget
import com.felixbrucker.tracer.core.model.PingResult
import com.felixbrucker.tracer.core.model.PingSummary
import com.felixbrucker.tracer.core.model.TraceMode
import com.felixbrucker.tracer.core.model.TraceSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PingUiState(
    val targetHost: String = "",
    val isRunning: Boolean = false,
    val results: List<PingResult> = emptyList(),
    val summary: PingSummary = PingSummary(),
    val settings: TraceSettings = TraceSettings(),
    val isFavorite: Boolean = false
)

@HiltViewModel
class PingViewModel @Inject constructor(
    private val networkToolRepository: NetworkToolRepository,
    private val settingsRepository: SettingsRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PingUiState())
    val uiState: StateFlow<PingUiState> = _uiState.asStateFlow()

    private var pingJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            _uiState.update { it.copy(settings = settings) }
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

    fun startPing() {
        val host = _uiState.value.targetHost.trim()
        if (host.isEmpty()) return

        pingJob?.cancel()
        _uiState.update { it.copy(isRunning = true, results = emptyList(), summary = PingSummary()) }

        pingJob = viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            networkToolRepository.executePingStream(host, settings)
                .collect { result ->
                    _uiState.update { currentState ->
                        val updatedResults = currentState.results + result
                        val updatedSummary = calculateSummary(updatedResults)
                        currentState.copy(
                            results = updatedResults,
                            summary = updatedSummary
                        )
                    }
                }
            _uiState.update { it.copy(isRunning = false) }
        }
    }

    fun stopPing() {
        pingJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    private fun calculateSummary(results: List<PingResult>): PingSummary {
        val transmitted = results.size
        val successful = results.filter { it.isSuccess && it.rttMs != null }
        val received = successful.size

        if (received == 0) {
            return PingSummary(transmitted = transmitted, received = 0)
        }

        val rtts = successful.mapNotNull { it.rttMs }
        val min = rtts.minOrNull()
        val max = rtts.maxOrNull()
        val avg = rtts.average().toFloat()

        val variance = rtts.map { (it - avg) * (it - avg) }.average()
        val stdev = kotlin.math.sqrt(variance).toFloat()

        return PingSummary(
            transmitted = transmitted,
            received = received,
            minRttMs = min,
            avgRttMs = avg,
            maxRttMs = max,
            stdevRttMs = stdev
        )
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
                        defaultMode = TraceMode.PING
                    )
                )
                _uiState.update { it.copy(isFavorite = true) }
            }
        }
    }
}
