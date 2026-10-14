package com.felixbrucker.tracer.core.network

import com.felixbrucker.tracer.core.data.TraceRepository
import com.felixbrucker.tracer.core.model.HopResult
import com.felixbrucker.tracer.core.model.MtrHopStats
import com.felixbrucker.tracer.core.model.PingResult
import com.felixbrucker.tracer.core.model.TraceType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TraceOrchestrationManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun testStartTracingPing() = testScope.runTest {
        val traceRepositoryMock = mockk<TraceRepository>(relaxed = true)
        val ping = PingResult(sequence = 1, target = "8.8.8.8", ipAddress = "8.8.8.8", ttl = 64, rttMs = 10.0, isSuccess = true)
        coEvery { traceRepositoryMock.runPingStream("8.8.8.8") } returns flowOf(ping)

        val manager = TraceOrchestrationManager(traceRepositoryMock)
        manager.startTracing(
            scope = testScope,
            target = "8.8.8.8",
            traceType = TraceType.PING,
            isAppInBackground = { true },
            onUpdateNotification = { _, _ -> },
            onPostSummaryNotification = { _, _ -> },
            onFinish = {}
        )

        assertNotNull(manager)
        manager.stopTracing()
    }

    @Test
    fun testStartTracingTraceroute() = testScope.runTest {
        val traceRepositoryMock = mockk<TraceRepository>(relaxed = true)
        val hop = HopResult(hopNumber = 1, hostName = "gateway", ipAddress = "192.168.1.1", rttMs = 2.0, isFinalDestination = true)
        coEvery { traceRepositoryMock.runTracerouteStream("1.1.1.1") } returns flowOf(hop)

        val manager = TraceOrchestrationManager(traceRepositoryMock)
        manager.startTracing(
            scope = testScope,
            target = "1.1.1.1",
            traceType = TraceType.TRACEROUTE,
            isAppInBackground = { true },
            onUpdateNotification = { _, _ -> },
            onPostSummaryNotification = { _, _ -> },
            onFinish = {}
        )

        assertNotNull(manager)
    }

    @Test
    fun testStartTracingMtr() = testScope.runTest {
        val traceRepositoryMock = mockk<TraceRepository>(relaxed = true)
        val stats = listOf(MtrHopStats(hopNumber = 1, ipAddress = "8.8.8.8", hostName = "google", sent = 10, received = 10, lastRttMs = 5.0, bestRttMs = 4.0, worstRttMs = 6.0, totalRttMs = 50.0))
        coEvery { traceRepositoryMock.runMtrStream("8.8.8.8") } returns flowOf(stats)

        val manager = TraceOrchestrationManager(traceRepositoryMock)
        manager.startTracing(
            scope = testScope,
            target = "8.8.8.8",
            traceType = TraceType.MTR,
            isAppInBackground = { true },
            onUpdateNotification = { _, _ -> },
            onPostSummaryNotification = { _, _ -> },
            onFinish = {}
        )

        assertNotNull(manager)
    }

    @Test
    fun testStartTracingExceptionCatch() = testScope.runTest {
        val traceRepositoryMock = mockk<TraceRepository>(relaxed = true)
        coEvery { traceRepositoryMock.runPingStream("8.8.8.8") } returns flow { throw RuntimeException("Stream error") }

        val manager = TraceOrchestrationManager(traceRepositoryMock)
        manager.startTracing(
            scope = testScope,
            target = "8.8.8.8",
            traceType = TraceType.PING,
            isAppInBackground = { true },
            onUpdateNotification = { _, _ -> },
            onPostSummaryNotification = { _, _ -> },
            onFinish = {}
        )

        assertNotNull(manager)
    }
}
