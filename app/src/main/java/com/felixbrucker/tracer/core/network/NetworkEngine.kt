package com.felixbrucker.tracer.core.network

import com.felixbrucker.tracer.core.model.HopResult
import com.felixbrucker.tracer.core.model.MtrHopStats
import com.felixbrucker.tracer.core.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkEngine @Inject constructor() {

    fun executePingStream(target: String, count: Int = Int.MAX_VALUE, intervalMs: Long = 1000L): Flow<PingResult> = flow {
        var sequence = 1
        val resolvedIp = resolveHost(target)

        while (currentCoroutineContext().isActive && sequence <= count) {
            val startTime = System.currentTimeMillis()
            val pingResult = runSinglePing(target, resolvedIp, sequence)
            emit(pingResult)

            sequence++
            val elapsed = System.currentTimeMillis() - startTime
            val sleepTime = intervalMs - elapsed
            if (sleepTime > 0) {
                delay(sleepTime)
            }
        }
    }.flowOn(Dispatchers.IO)

    fun executeTracerouteStream(target: String, maxHops: Int = 30): Flow<HopResult> = flow {
        val resolvedIp = resolveHost(target)
        var reachedDestination = false

        for (ttl in 1..maxHops) {
            if (!currentCoroutineContext().isActive || reachedDestination) break

            val hop = runSingleProbeWithTtl(target, ttl)
            emit(hop)

            if (hop.isFinalDestination || (resolvedIp != null && hop.ipAddress == resolvedIp)) {
                reachedDestination = true
            }
        }
    }.flowOn(Dispatchers.IO)

    fun executeWinMtrStream(target: String, maxHops: Int = 30, intervalMs: Long = 1000L): Flow<List<MtrHopStats>> = flow {
        val statsMap = mutableMapOf<Int, MtrHopStats>()
        for (i in 1..maxHops) {
            statsMap[i] = MtrHopStats(hopNumber = i)
        }

        var activeMaxHops = 15

        while (currentCoroutineContext().isActive) {
            val cycleStartTime = System.currentTimeMillis()

            for (ttl in 1..activeMaxHops) {
                if (!currentCoroutineContext().isActive) break

                val probe = runSingleProbeWithTtl(target, ttl)
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
                    activeMaxHops = ttl
                    break
                }
            }

            val currentStatsList = statsMap.values
                .filter { it.hopNumber <= activeMaxHops || it.sent > 0 }
                .sortedBy { it.hopNumber }

            emit(currentStatsList)

            val elapsed = System.currentTimeMillis() - cycleStartTime
            val sleepTime = intervalMs - elapsed
            if (sleepTime > 0) {
                delay(sleepTime)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun runSinglePing(target: String, resolvedIp: String?, sequence: Int): PingResult {
        return try {
            val startMs = System.currentTimeMillis()
            val process = Runtime.getRuntime().exec("ping -c 1 -w 2 $target")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var rttMs: Double? = null
            var ip: String? = resolvedIp

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.contains("bytes from") || currentLine.contains("from")) {
                    rttMs = parseRttFromPingOutput(currentLine)
                    if (ip == null) {
                        ip = parseIpFromPingOutput(currentLine)
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
                isSuccess = exitCode == 0 && rttMs != null
            )
        } catch (e: Exception) {
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

    private fun runSingleProbeWithTtl(target: String, ttl: Int): HopResult {
        return try {
            val startMs = System.currentTimeMillis()
            val process = Runtime.getRuntime().exec("ping -c 1 -t $ttl -w 2 $target")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var ipAddress: String? = null
            var rttMs: Double? = null
            var isDestination = false
            var receivedResponse = false

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.contains("From") || currentLine.contains("from") || currentLine.contains("bytes from")) {
                    receivedResponse = true
                    ipAddress = parseIpFromPingOutput(currentLine)
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
                val reverseName = reverseLookup(ipAddress)
                HopResult(
                    hopNumber = ttl,
                    hostName = reverseName ?: ipAddress,
                    ipAddress = ipAddress,
                    rttMs = rttMs,
                    isFinalDestination = isDestination,
                    isTimeout = false
                )
            }
        } catch (e: Exception) {
            HopResult(
                hopNumber = ttl,
                hostName = "*",
                ipAddress = "*",
                rttMs = null,
                isTimeout = true
            )
        }
    }

    private fun reverseLookup(ip: String): String? {
        return try {
            val inet = InetAddress.getByName(ip)
            val hostName = inet.canonicalHostName
            if (hostName != ip) hostName else null
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveHost(host: String): String? {
        return try {
            InetAddress.getByName(host).hostAddress
        } catch (e: Exception) {
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

    private fun parseIpFromPingOutput(line: String): String? {
        return try {
            val regex = """\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b""".toRegex()
            regex.find(line)?.value
        } catch (e: Exception) {
            null
        }
    }
}
