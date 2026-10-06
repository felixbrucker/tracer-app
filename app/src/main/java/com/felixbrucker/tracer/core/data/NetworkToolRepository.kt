package com.felixbrucker.tracer.core.data

import com.felixbrucker.tracer.core.model.HopStats
import com.felixbrucker.tracer.core.model.PingResult
import com.felixbrucker.tracer.core.model.TraceSettings
import com.felixbrucker.tracer.core.model.TracerouteHop
import com.felixbrucker.tracer.core.network.NetworkExecutionEngine
import com.felixbrucker.tracer.core.util.DnsResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkToolRepository @Inject constructor(
    private val executionEngine: NetworkExecutionEngine,
    private val dnsResolver: DnsResolver
) {

    fun executePingStream(
        host: String,
        settings: TraceSettings
    ): Flow<PingResult> = executionEngine.executePingStream(host, settings)

    fun executeTracerouteStream(
        targetHost: String,
        settings: TraceSettings
    ): Flow<TracerouteHop> = flow {
        val destinationIp = dnsResolver.resolveHost(targetHost) ?: targetHost
        var isTargetReached = false

        for (ttl in 1..settings.maxHops) {
            if (isTargetReached) break

            val probe1 = executionEngine.probeSingleHop(targetHost, ttl, settings.timeoutMs, settings.packetSize)
            val probe2 = executionEngine.probeSingleHop(targetHost, ttl, settings.timeoutMs, settings.packetSize)
            val probe3 = executionEngine.probeSingleHop(targetHost, ttl, settings.timeoutMs, settings.packetSize)

            val ip = probe1.ip ?: probe2.ip ?: probe3.ip
            val hostDisplay = if (ip != null) {
                if (settings.resolveDns) dnsResolver.resolveReverse(ip) else ip
            } else {
                "*"
            }

            if (probe1.isDestinationReached || probe2.isDestinationReached || probe3.isDestinationReached || ip == destinationIp) {
                isTargetReached = true
            }

            val hop = TracerouteHop(
                hopNumber = ttl,
                host = hostDisplay,
                ip = ip,
                rtt1Ms = probe1.rttMs,
                rtt2Ms = probe2.rttMs,
                rtt3Ms = probe3.rttMs,
                isReached = isTargetReached
            )
            emit(hop)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun probeHop(
        targetHost: String,
        ttl: Int,
        settings: TraceSettings
    ): HopProbeData = withContext(Dispatchers.IO) {
        val probe = executionEngine.probeSingleHop(targetHost, ttl, settings.timeoutMs, settings.packetSize)
        val hostDisplay = if (probe.ip != null) {
            if (settings.resolveDns) dnsResolver.resolveReverse(probe.ip) else probe.ip
        } else {
            "*"
        }
        HopProbeData(
            hopNumber = ttl,
            host = hostDisplay,
            ip = probe.ip,
            rttMs = probe.rttMs,
            isReached = probe.isDestinationReached
        )
    }
}

data class HopProbeData(
    val hopNumber: Int,
    val host: String,
    val ip: String?,
    val rttMs: Float?,
    val isReached: Boolean
)
