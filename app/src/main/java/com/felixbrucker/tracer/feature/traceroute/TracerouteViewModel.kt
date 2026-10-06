package com.felixbrucker.tracer.feature.traceroute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felixbrucker.tracer.core.data.FavoriteRepository
import com.felixbrucker.tracer.core.data.NetworkToolRepository
import com.felixbrucker.tracer.core.datastore.SettingsRepository
import com.felixbrucker.tracer.core.model.FavoriteTarget
import com.felixbrucker.tracer.core.model.TraceMode
import com.felixbrucker.tracer.core.model.TraceSettings
import com.felixbrucker.tracer.core.model.TracerouteHop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TracerouteUiState(
    val targetHost: String = "",
    val isRunning: Boolean = false,
    val hops: List<TracerouteHop> = emptyList(),
    val settings: TraceSettings = TraceSettings(),
    val isFavorite: Boolean = false
)

@HiltViewModel
class TracerouteViewModel @Inject constructor(
    private val networkToolRepository: NetworkToolRepository,
    private val settingsRepository: SettingsRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TracerouteUiState())
    val uiState: StateFlow<TracerouteUiState> = _uiState.asStateFlow()

    private var traceJob: Job? = null

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

    fun startTraceroute() {
        val host = _uiState.value.targetHost.trim()
        if (host.isEmpty()) return

        traceJob?.cancel()
        _uiState.update { it.copy(isRunning = true, hops = emptyList()) }

        traceJob = viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            networkToolRepository.executeTracerouteStream(host, settings)
                .collect { hop ->
                    _uiState.update { currentState ->
                        currentState.copy(hops = currentState.hops + hop)
                    }
                }
            _uiState.update { it.copy(isRunning = false) }
        }
    }

    fun stopTraceroute() {
        traceJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
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
                        defaultMode = TraceMode.REGULAR_TRACEROUTE
                    )
                )
                _uiState.update { it.copy(isFavorite = true) }
            }
        }
    }
}
