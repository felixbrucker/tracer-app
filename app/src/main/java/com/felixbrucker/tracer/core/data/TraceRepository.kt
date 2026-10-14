package com.felixbrucker.tracer.core.data

import com.felixbrucker.tracer.core.datastore.AppSettingsRepository
import com.felixbrucker.tracer.core.model.HopResult
import com.felixbrucker.tracer.core.model.MtrHopStats
import com.felixbrucker.tracer.core.model.PingResult
import com.felixbrucker.tracer.core.model.TraceType
import com.felixbrucker.tracer.core.network.NetworkEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraceRepository @Inject constructor(
    private val networkEngine: NetworkEngine,
    private val appSettingsRepository: AppSettingsRepository
) {

    private val _pingResults = MutableStateFlow<List<PingResult>>(emptyList())
    val pingResults: StateFlow<List<PingResult>> = _pingResults.asStateFlow()

    private val _tracerouteHops = MutableStateFlow<List<HopResult>>(emptyList())
    val tracerouteHops: StateFlow<List<HopResult>> = _tracerouteHops.asStateFlow()

    private val _mtrStats = MutableStateFlow<List<MtrHopStats>>(emptyList())
    val mtrStats: StateFlow<List<MtrHopStats>> = _mtrStats.asStateFlow()

    private val _activeTraceType = MutableStateFlow<TraceType?>(null)
    val activeTraceType: StateFlow<TraceType?> = _activeTraceType.asStateFlow()

    private val _activeTarget = MutableStateFlow<String?>(null)
    val activeTarget: StateFlow<String?> = _activeTarget.asStateFlow()

    suspend fun runPingStream(target: String): Flow<PingResult> {
        val settings = appSettingsRepository.tracerSettings.first()
        _activeTarget.value = target
        _activeTraceType.value = TraceType.PING
        _pingResults.value = emptyList()
        return networkEngine.executePingStream(target, settings = settings)
    }

    suspend fun runTracerouteStream(target: String): Flow<HopResult> {
        val settings = appSettingsRepository.tracerSettings.first()
        _activeTarget.value = target
        _activeTraceType.value = TraceType.TRACEROUTE
        _tracerouteHops.value = emptyList()
        return networkEngine.executeTracerouteStream(target, settings = settings)
    }

    suspend fun runMtrStream(target: String): Flow<List<MtrHopStats>> {
        val settings = appSettingsRepository.tracerSettings.first()
        _activeTarget.value = target
        _activeTraceType.value = TraceType.MTR
        _mtrStats.value = emptyList()
        return networkEngine.executeMtrStream(target, settings = settings)
    }

    fun updatePingResult(result: PingResult) {
        val currentList = _pingResults.value.toMutableList()
        currentList.add(result)
        _pingResults.value = currentList
    }

    fun updateTracerouteHop(hop: HopResult) {
        val currentList = _tracerouteHops.value.toMutableList()
        currentList.add(hop)
        _tracerouteHops.value = currentList
    }

    fun updateMtrStats(stats: List<MtrHopStats>) {
        _mtrStats.value = stats
    }

    fun clearActiveTrace() {
        _activeTraceType.value = null
        _activeTarget.value = null
    }
}
