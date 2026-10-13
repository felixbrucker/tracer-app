package com.felixbrucker.tracer.core.data

import com.felixbrucker.tracer.core.model.HopResult
import com.felixbrucker.tracer.core.model.MtrHopStats
import com.felixbrucker.tracer.core.model.PingResult
import com.felixbrucker.tracer.core.model.TraceType
import com.felixbrucker.tracer.core.network.NetworkEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraceRepository @Inject constructor(
    private val networkEngine: NetworkEngine
) {

    private val _pingResults = MutableStateFlow<List<PingResult>>(emptyList())
    val pingResults: StateFlow<List<PingResult>> = _pingResults.asStateFlow()

    private val _tracerouteHops = MutableStateFlow<List<HopResult>>(emptyList())
    val tracerouteHops: StateFlow<List<HopResult>> = _tracerouteHops.asStateFlow()

    private val _winMtrStats = MutableStateFlow<List<MtrHopStats>>(emptyList())
    val winMtrStats: StateFlow<List<MtrHopStats>> = _winMtrStats.asStateFlow()

    private val _activeTraceType = MutableStateFlow<TraceType?>(null)
    val activeTraceType: StateFlow<TraceType?> = _activeTraceType.asStateFlow()

    private val _activeTarget = MutableStateFlow<String?>(null)
    val activeTarget: StateFlow<String?> = _activeTarget.asStateFlow()

    fun runPingStream(target: String): Flow<PingResult> {
        _activeTarget.value = target
        _activeTraceType.value = TraceType.PING
        _pingResults.value = emptyList()
        return networkEngine.executePingStream(target)
    }

    fun runTracerouteStream(target: String): Flow<HopResult> {
        _activeTarget.value = target
        _activeTraceType.value = TraceType.TRACEROUTE
        _tracerouteHops.value = emptyList()
        return networkEngine.executeTracerouteStream(target)
    }

    fun runWinMtrStream(target: String): Flow<List<MtrHopStats>> {
        _activeTarget.value = target
        _activeTraceType.value = TraceType.MTR
        _winMtrStats.value = emptyList()
        return networkEngine.executeWinMtrStream(target)
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

    fun updateWinMtrStats(stats: List<MtrHopStats>) {
        _winMtrStats.value = stats
    }

    fun clearActiveTrace() {
        _activeTraceType.value = null
        _activeTarget.value = null
    }
}
