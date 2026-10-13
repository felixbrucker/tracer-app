package com.felixbrucker.tracer.core.util

import com.felixbrucker.tracer.core.model.HopResult
import com.felixbrucker.tracer.core.model.MtrHopStats
import com.felixbrucker.tracer.core.model.PingResult
import org.junit.Assert.assertNotNull
import org.junit.Test

class TraceSummaryCalculatorTest {

    @Test
    fun testPingCalculations() {
        val ping = PingResult(sequence = 1, target = "8.8.8.8", ipAddress = "8.8.8.8", ttl = 64, rttMs = 12.0, isSuccess = true)
        val list = listOf(ping)
        val status = TraceSummaryCalculator.calculatePingStatus(ping, list)
        assertNotNull(status)

        val summary = TraceSummaryCalculator.calculatePingFinalSummary(list)
        assertNotNull(summary)
    }

    @Test
    fun testTracerouteCalculations() {
        val hop = HopResult(hopNumber = 1, hostName = "gateway", ipAddress = "192.168.1.1", rttMs = 2.0, isFinalDestination = true)
        val hops = listOf(hop)
        val status = TraceSummaryCalculator.calculateTracerouteStatus(hop)
        assertNotNull(status)

        val summary = TraceSummaryCalculator.calculateTracerouteFinalSummary(hops)
        assertNotNull(summary)
    }

    @Test
    fun testMtrCalculations() {
        val stats = listOf(
            MtrHopStats(hopNumber = 1, ipAddress = "8.8.8.8", hostName = "google", sent = 10, received = 10, lastRttMs = 5.0, bestRttMs = 4.0, worstRttMs = 6.0, totalRttMs = 50.0)
        )
        val status = TraceSummaryCalculator.calculateMtrStatus(stats)
        assertNotNull(status)

        val summary = TraceSummaryCalculator.calculateMtrFinalSummary(stats)
        assertNotNull(summary)
    }
}
