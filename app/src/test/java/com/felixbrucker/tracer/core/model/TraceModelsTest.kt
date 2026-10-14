package com.felixbrucker.tracer.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TraceModelsTest {

    @Test
    fun testMtrHopStatsComputedProperties() {
        val statsWithZeroSent = MtrHopStats(hopNumber = 1, sent = 0, received = 0)
        assertEquals(0.0, statsWithZeroSent.lossPercentage, 0.001)
        assertEquals(0.0, statsWithZeroSent.avgRttMs, 0.001)

        val statsWithZeroReceived = MtrHopStats(hopNumber = 1, sent = 10, received = 0, totalRttMs = 0.0)
        assertEquals(100.0, statsWithZeroReceived.lossPercentage, 0.001)
        assertEquals(0.0, statsWithZeroReceived.avgRttMs, 0.001)

        val statsNormal = MtrHopStats(hopNumber = 1, sent = 10, received = 8, totalRttMs = 160.0)
        assertEquals(20.0, statsNormal.lossPercentage, 0.001)
        assertEquals(20.0, statsNormal.avgRttMs, 0.001)
    }
}
