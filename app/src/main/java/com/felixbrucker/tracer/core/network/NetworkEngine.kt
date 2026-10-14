package com.felixbrucker.tracer.core.network

import com.felixbrucker.tracer.core.datastore.TracerSettings
import com.felixbrucker.tracer.core.model.HopResult
import com.felixbrucker.tracer.core.model.MtrHopStats
import com.felixbrucker.tracer.core.model.PingResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class NetworkEngine @Inject constructor() {

    private val reverseDnsCache = ConcurrentHashMap<String, String>()

    fun executePingStream(
        target: String,
        count: Int = Int.MAX_VALUE,
        settings: TracerSettings = TracerSettings()
    ): Flow<PingResult> = flow {
        var sequence = 1
        val resolvedIp = resolveHost(target)

        while (currentCoroutineContext().isActive && sequence <= count) {
            val startTime = System.currentTimeMillis()
            val pingResult = runSinglePing(target, resolvedIp, sequence, settings.pingTimeoutSec)
            emit(pingResult)

            sequence++
            val elapsed = System.currentTimeMillis() - startTime
            val sleepTime = settings.pingIntervalMs - elapsed
            if (sleepTime > 0) {
                delay(sleepTime.milliseconds)
            }
        }
    }.flowOn(Dispatchers.IO)

    fun executeTracerouteStream(
        target: String,
        settings: TracerSettings = TracerSettings()
    ): Flow<HopResult> = flow {
        val resolvedIp = resolveHost(target)
        coroutineScope {
            val probeJobs = (1..settings.maxHops).map { ttl ->
                async {
                    runSingleProbeWithTtl(target, ttl, settings.pingTimeoutSec)
                }
            }

            var reachedDestination = false
            for (deferred in probeJobs) {
                if (!currentCoroutineContext().isActive || reachedDestination) {
                    probeJobs.forEach { it.cancel() }
                    break
                }
                val hop = deferred.await()
                emit(hop)

                if (hop.isFinalDestination || (resolvedIp != null && hop.ipAddress == resolvedIp && hop.ipAddress != "*")) {
                    reachedDestination = true
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun executeMtrStream(
        target: String,
        settings: TracerSettings = TracerSettings()
    ): Flow<List<MtrHopStats>> = flow {
        val statsMap = mutableMapOf<Int, MtrHopStats>()
        for (i in 1..settings.maxHops) {
            statsMap[i] = MtrHopStats(hopNumber = i)
        }

        var activeMaxHops = minOf(15, settings.maxHops)

        while (currentCoroutineContext().isActive) {
            val cycleStartTime = System.currentTimeMillis()

            val probes = coroutineScope {
                (1..activeMaxHops).map { ttl ->
                    async {
                        ttl to runSingleProbeWithTtl(target, ttl, settings.pingTimeoutSec)
                    }
                }.awaitAll()
            }

            var foundFinal = false
            var finalTtl = activeMaxHops

            for ((ttl, probe) in probes) {
                val currentStat = statsMap[ttl] ?: MtrHopStats(hopNumber = ttl)

                val newSent = currentStat.sent + 1
                val isSuccess = !probe.isTimeout
                val newReceived = if (isSuccess) currentStat.received + 1 else currentStat.received

                val rtt = probe.rttMs ?: 0.0
                val newLast = if (isSuccess) rtt else currentStat.lastRttMs
                val newBest = if (isSuccess && rtt > 0.0) minOf(currentStat.bestRttMs, rtt) else currentStat.bestRttMs
                val newWorst = if (isSuccess) maxOf(currentStat.worstRttMs, rtt) else currentStat.worstRttMs
                val newTotal = if (isSuccess) currentStat.totalRttMs + rtt else currentStat.totalRttMs

                val newIp = if (!probe.isTimeout && probe.ipAddress != "*") probe.ipAddress else currentStat.ipAddress
                val newHost = if (!probe.isTimeout && probe.hostName != "*") probe.hostName else currentStat.hostName

                val updatedStat = currentStat.copy(
                    ipAddress = newIp ?: "*",
                    hostName = newHost ?: "*",
                    sent = newSent,
                    received = newReceived,
                    lastRttMs = newLast,
                    bestRttMs = newBest,
                    worstRttMs = newWorst,
                    totalRttMs = newTotal
                )

                statsMap[ttl] = updatedStat

                if (probe.isFinalDestination) {
                    foundFinal = true
                    finalTtl = ttl
                    break
                }
            }

            if (foundFinal) {
                activeMaxHops = finalTtl
            } else if (activeMaxHops < settings.maxHops) {
                activeMaxHops = minOf(activeMaxHops + 5, settings.maxHops)
            }

            val currentStatsList = statsMap.values
                .filter { it.hopNumber <= activeMaxHops || it.sent > 0 }
                .sortedBy { it.hopNumber }

            emit(currentStatsList)

            val elapsed = System.currentTimeMillis() - cycleStartTime
            val sleepTime = settings.pingIntervalMs - elapsed
            if (sleepTime > 0) {
                delay(sleepTime.milliseconds)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun runSinglePing(target: String, resolvedIp: String?, sequence: Int, timeoutSec: Int): PingResult {
        return try {
            val startMs = System.currentTimeMillis()
            val process = Runtime.getRuntime().exec("ping -c 1 -w $timeoutSec $target")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var rttMs: Double? = null
            var ip: String? = resolvedIp

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.contains("bytes from") || currentLine.contains("from")) {
                    rttMs = parseRttFromPingOutput(currentLine)
                    if (ip == null) {
                        ip = parseIpFromPingOutput(currentLine)?.second
                    }
                }
            }
            val exitCode = process.waitFor()
            reader.close()

            val endMs = System.currentTimeMillis()
            if (rttMs == null && exitCode == 0) {
                rttMs = (endMs - startMs).toDouble()
            }

            PingResult(
                sequence = sequence,
                target = target,
                ipAddress = ip,
                ttl = 64,
                rttMs = if (exitCode == 0) rttMs else null,
                isSuccess = exitCode == 0
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            PingResult(
                sequence = sequence,
                target = target,
                ipAddress = resolvedIp,
                ttl = 64,
                rttMs = null,
                isSuccess = false
            )
        }
    }

    private fun runSingleProbeWithTtl(target: String, ttl: Int, timeoutSec: Int): HopResult {
        return try {
            val startMs = System.currentTimeMillis()
            val process = Runtime.getRuntime().exec("ping -c 1 -t $ttl -w $timeoutSec $target")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var ipAddress: String? = null
            var parsedHost: String? = null
            var rttMs: Double? = null
            var isDestination = false
            var receivedResponse = false

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.contains("From") || currentLine.contains("from") || currentLine.contains("bytes from")) {
                    receivedResponse = true
                    val hostAndIp = parseIpFromPingOutput(currentLine)
                    if (hostAndIp != null) {
                        parsedHost = hostAndIp.first
                        ipAddress = hostAndIp.second
                    }
                    rttMs = parseRttFromPingOutput(currentLine)
                }
                if (currentLine.contains("bytes from") && !currentLine.contains("Time to live exceeded")) {
                    isDestination = true
                }
            }
            process.waitFor()
            reader.close()

            val endMs = System.currentTimeMillis()
            if (receivedResponse && rttMs == null) {
                rttMs = (endMs - startMs).toDouble()
            }

            if (!receivedResponse || ipAddress == null) {
                HopResult(
                    hopNumber = ttl,
                    hostName = "*",
                    ipAddress = "*",
                    rttMs = null,
                    isTimeout = true
                )
            } else {
                val resolvedHostName = resolveReverseName(ipAddress, parsedHost)
                HopResult(
                    hopNumber = ttl,
                    hostName = resolvedHostName,
                    ipAddress = ipAddress,
                    rttMs = rttMs,
                    isFinalDestination = isDestination,
                    isTimeout = false
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            HopResult(
                hopNumber = ttl,
                hostName = "*",
                ipAddress = "*",
                rttMs = null,
                isTimeout = true
            )
        }
    }

    private fun resolveReverseName(ip: String, parsedHost: String?): String {
        if (!parsedHost.isNullOrBlank() && parsedHost != ip) {
            reverseDnsCache[ip] = parsedHost
            return parsedHost
        }
        reverseDnsCache[ip]?.let { return it }

        return try {
            val inet = InetAddress.getByName(ip)
            val hostName = inet.canonicalHostName
            if (!hostName.isNullOrBlank() && hostName != ip) {
                reverseDnsCache[ip] = hostName
                hostName
            } else {
                ip
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ip
        }
    }

    private fun resolveHost(host: String): String? {
        return try {
            InetAddress.getByName(host).hostAddress
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    private fun parseRttFromPingOutput(line: String): Double? {
        return try {
            val timeIndex = line.indexOf("time=")
            if (timeIndex != -1) {
                val sub = line.substring(timeIndex + 5)
                val spaceIndex = sub.indexOf(" ")
                val timeStr = if (spaceIndex != -1) sub.substring(0, spaceIndex) else sub
                timeStr.toDoubleOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseIpFromPingOutput(line: String): Pair<String, String>? {
        return try {
            val regexWithHost = """from\s+([^\s()]+)(?:\s+\(([0-9.]+)\))?""".toRegex(RegexOption.IGNORE_CASE)
            val match = regexWithHost.find(line)
            if (match != null) {
                val firstGroup = match.groupValues[1].trimEnd(':')
                val secondGroup = match.groupValues.getOrNull(2)
                if (!secondGroup.isNullOrBlank()) {
                    Pair(firstGroup, secondGroup)
                } else {
                    Pair(firstGroup, firstGroup)
                }
            } else {
                val ipRegex = """\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b""".toRegex()
                val ipMatch = ipRegex.find(line)?.value
                if (ipMatch != null) Pair(ipMatch, ipMatch) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
