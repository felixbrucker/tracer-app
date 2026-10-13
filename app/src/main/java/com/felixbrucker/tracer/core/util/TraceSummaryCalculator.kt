package com.felixbrucker.tracer.core.util

import com.felixbrucker.tracer.core.model.HopResult
import com.felixbrucker.tracer.core.model.MtrHopStats
import com.felixbrucker.tracer.core.model.PingResult

object TraceSummaryCalculator {

    fun calculatePingStatus(pingResult: PingResult, pingList: List<PingResult>): String {
        val sent = pingList.size
        val recv = pingList.count { it.isSuccess }
        val lost = sent - recv
        val lossPct = if (sent == 0) 0.0 else (lost.toDouble() / sent.toDouble()) * 100.0
        return if (pingResult.isSuccess) {
            "Seq ${pingResult.sequence}: ${pingResult.rttMs?.let { "%.1f".format(it) }} ms | Recv: $recv/$sent (Loss: ${"%.0f".format(lossPct)}%)"
        } else {
            "Seq ${pingResult.sequence}: Timed out | Recv: $recv/$sent (Loss: ${"%.0f".format(lossPct)}%)"
        }
    }

    fun calculatePingFinalSummary(pingList: List<PingResult>): String {
        val sent = pingList.size
        val recv = pingList.count { it.isSuccess }
        val lost = sent - recv
        val lossPct = if (sent == 0) 0.0 else (lost.toDouble() / sent.toDouble()) * 100.0
        val rtts = pingList.mapNotNull { it.rttMs }
        val avgRtt = if (rtts.isNotEmpty()) rtts.average() else 0.0
        val minRtt = rtts.minOrNull() ?: 0.0
        val maxRtt = rtts.maxOrNull() ?: 0.0
        return "Sent: $sent, Recv: $recv, Lost: $lost (${"%.0f".format(lossPct)}% loss) | RTT Min/Avg/Max: ${"%.1f".format(minRtt)}/${"%.1f".format(avgRtt)}/${"%.1f".format(maxRtt)} ms"
    }

    fun calculateTracerouteStatus(hop: HopResult): String {
        return "Hop ${hop.hopNumber}: ${hop.ipAddress} (${if (hop.isTimeout) "Timeout" else "${hop.rttMs?.let { "%.1f".format(it) }} ms"})"
    }

    fun calculateTracerouteFinalSummary(hops: List<HopResult>): String {
        val reached = hops.lastOrNull()?.isFinalDestination == true
        val rtts = hops.mapNotNull { it.rttMs }
        val avgRtt = if (rtts.isNotEmpty()) rtts.average() else 0.0
        val destHop = hops.lastOrNull()
        return if (reached && destHop != null) {
            "Destination reached in ${hops.size} hops | Final RTT: ${"%.1f".format(destHop.rttMs ?: 0.0)} ms | Path Avg RTT: ${"%.1f".format(avgRtt)} ms"
        } else {
            "Stopped after ${hops.size} hops | Path Avg RTT: ${"%.1f".format(avgRtt)} ms"
        }
    }

    fun calculateMtrStatus(stats: List<MtrHopStats>): String {
        val destHop = stats.lastOrNull { it.sent > 0 }
        val totalProbes = stats.maxOfOrNull { it.sent } ?: 0
        return if (destHop != null) {
            "Probes: $totalProbes | Hops: ${stats.size} | Loss: ${"%.0f".format(destHop.lossPercentage)}% | Last RTT: ${"%.1f".format(destHop.lastRttMs)} ms"
        } else {
            "Tracing path (Probes: $totalProbes)..."
        }
    }

    fun calculateMtrFinalSummary(stats: List<MtrHopStats>): String {
        val destHop = stats.lastOrNull { it.sent > 0 }
        val totalProbes = stats.maxOfOrNull { it.sent } ?: 0
        return if (destHop != null) {
            "Probes: $totalProbes | Hops: ${stats.size} | Final Loss: ${"%.0f".format(destHop.lossPercentage)}% | Avg RTT: ${"%.1f".format(destHop.avgRttMs)} ms | Best/Worst: ${"%.0f".format(destHop.bestRttMs)}/${"%.0f".format(destHop.worstRttMs)} ms"
        } else {
            "Session ended after $totalProbes probes (${stats.size} hops)"
        }
    }
}
