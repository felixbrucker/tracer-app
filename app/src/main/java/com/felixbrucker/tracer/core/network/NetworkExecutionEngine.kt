package com.felixbrucker.tracer.core.network

import com.felixbrucker.tracer.core.model.PingResult
import com.felixbrucker.tracer.core.model.TraceSettings
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class HopProbeResult(
    val hopNumber: Int,
    val ip: String?,
    val rttMs: Float?,
    val isDestinationReached: Boolean = false
)

@Singleton
class NetworkExecutionEngine @Inject constructor() {

    suspend fun probeSingleHop(
        host: String,
        ttl: Int,
        timeoutMs: Int = 1000,
        bytes: Int = 64
    ): HopProbeResult = withContext(Dispatchers.IO) {
        val startTime = System.nanoTime()
        val processBuilder = ProcessBuilder(
            "ping",
            "-c", "1",
            "-t", ttl.toString(),
            "-W", (timeoutMs / 1000).coerceAtLeast(1).toString(),
            "-s", bytes.toString(),
            host
        )
        processBuilder.redirectErrorStream(true)

        runCatching {
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var responderIp: String? = null
            var rtt: Float? = null
            var reached = false

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: break
                if (currentLine.contains("Time to live exceeded") || currentLine.contains("Time Exceeded")) {
                    responderIp = extractIpFromLine(currentLine)
                } else if (currentLine.contains("bytes from")) {
                    responderIp = extractIpFromLine(currentLine)
                    rtt = extractRttFromLine(currentLine)
                    reached = true
                }
            }
            process.waitFor()
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000f

            if (rtt == null && responderIp != null) {
                rtt = elapsedMs
            }

            HopProbeResult(
                hopNumber = ttl,
                ip = responderIp,
                rttMs = if (responderIp != null) rtt else null,
                isDestinationReached = reached
            )
        }.getOrElse {
            fallbackTcpSocketProbe(host, ttl, timeoutMs)
        }
    }

    private fun fallbackTcpSocketProbe(host: String, ttl: Int, timeoutMs: Int): HopProbeResult {
        val startTime = System.currentTimeMillis()
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, 80), timeoutMs)
            val elapsed = (System.currentTimeMillis() - startTime).toFloat()
            socket.close()
            HopProbeResult(ttl, host, elapsed, isDestinationReached = true)
        } catch (e: Exception) {
            HopProbeResult(ttl, null, null, isDestinationReached = false)
        }
    }

    fun executePingStream(
        host: String,
        settings: TraceSettings,
        count: Int = Int.MAX_VALUE
    ): Flow<PingResult> = flow {
        var seq = 1
        while (seq <= count) {
            val probe = probeSingleHop(
                host = host,
                ttl = 64,
                timeoutMs = settings.timeoutMs,
                bytes = settings.packetSize
            )
            val pingResult = PingResult(
                sequence = seq,
                bytes = settings.packetSize,
                ip = probe.ip ?: host,
                ttl = 64,
                rttMs = probe.rttMs,
                isSuccess = probe.rttMs != null
            )
            emit(pingResult)
            seq++
            if (seq <= count) {
                delay(settings.intervalMs)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun extractIpFromLine(line: String): String? {
        val ipPattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})")
        val matcher = ipPattern.matcher(line)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractRttFromLine(line: String): Float? {
        val timePattern = Pattern.compile("time=([0-9.]+)")
        val matcher = timePattern.matcher(line)
        return if (matcher.find()) matcher.group(1)?.toFloatOrNull() else null
    }
}
