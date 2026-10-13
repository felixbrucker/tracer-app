package com.felixbrucker.tracer.core.data

import com.felixbrucker.tracer.core.datastore.AppSettingsRepository
import com.felixbrucker.tracer.core.datastore.TracerSettings
import com.felixbrucker.tracer.core.model.HopResult
import com.felixbrucker.tracer.core.model.MtrHopStats
import com.felixbrucker.tracer.core.model.PingResult
import com.felixbrucker.tracer.core.model.TraceType
import com.felixbrucker.tracer.core.network.NetworkEngine
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TraceRepositoryTest {

    @Test
    fun testRunPingStreamSuccess() = runTest {
        val networkEngineMock = mockk<NetworkEngine>()
        val appSettingsRepoMock = mockk<AppSettingsRepository>()
        val expectedPing = PingResult(sequence = 1, target = "8.8.8.8", ipAddress = "8.8.8.8", ttl = 54, rttMs = 12.0, isSuccess = true)
        coEvery { appSettingsRepoMock.tracerSettings } returns flowOf(TracerSettings())
        every { networkEngineMock.executePingStream("8.8.8.8", any()) } returns flowOf(expectedPing)

        val repository = TraceRepository(networkEngineMock, appSettingsRepoMock)
        repository.runPingStream("8.8.8.8")

        assertEquals(TraceType.PING, repository.activeTraceType.value)
        assertEquals("8.8.8.8", repository.activeTarget.value)
    }

    @Test
    fun testRunTracerouteStreamSuccess() = runTest {
        val networkEngineMock = mockk<NetworkEngine>()
        val appSettingsRepoMock = mockk<AppSettingsRepository>()
        val hop = HopResult(hopNumber = 1, hostName = "gateway", ipAddress = "192.168.1.1", rttMs = 2.0, isFinalDestination = false, isTimeout = false)
        coEvery { appSettingsRepoMock.tracerSettings } returns flowOf(TracerSettings())
        every { networkEngineMock.executeTracerouteStream("1.1.1.1", any()) } returns flowOf(hop)

        val repository = TraceRepository(networkEngineMock, appSettingsRepoMock)
        repository.runTracerouteStream("1.1.1.1")

        assertEquals(TraceType.TRACEROUTE, repository.activeTraceType.value)
        assertEquals("1.1.1.1", repository.activeTarget.value)
    }

    @Test
    fun testRunWinMtrStreamSuccess() = runTest {
        val networkEngineMock = mockk<NetworkEngine>()
        val appSettingsRepoMock = mockk<AppSettingsRepository>()
        val stats = listOf(MtrHopStats(hopNumber = 1, ipAddress = "1.1.1.1", hostName = "cloudflare", sent = 10, received = 10, lastRttMs = 2.0, bestRttMs = 1.0, worstRttMs = 3.0, totalRttMs = 20.0))
        coEvery { appSettingsRepoMock.tracerSettings } returns flowOf(TracerSettings())
        every { networkEngineMock.executeWinMtrStream("8.8.8.8", any()) } returns flowOf(stats)

        val repository = TraceRepository(networkEngineMock, appSettingsRepoMock)
        repository.runWinMtrStream("8.8.8.8")

        assertEquals(TraceType.MTR, repository.activeTraceType.value)
        assertEquals("8.8.8.8", repository.activeTarget.value)
    }

    @Test
    fun testUpdatesAndClearActiveTrace() = runTest {
        val networkEngineMock = mockk<NetworkEngine>()
        val appSettingsRepoMock = mockk<AppSettingsRepository>()
        val repository = TraceRepository(networkEngineMock, appSettingsRepoMock)

        val ping = PingResult(sequence = 1, target = "8.8.8.8", ipAddress = "8.8.8.8", ttl = 64, rttMs = 10.0, isSuccess = true)
        repository.updatePingResult(ping)
        assertEquals(1, repository.pingResults.value.size)

        val hop = HopResult(hopNumber = 1, hostName = "host", ipAddress = "10.0.0.1", rttMs = 1.0)
        repository.updateTracerouteHop(hop)
        assertEquals(1, repository.tracerouteHops.value.size)

        val stats = listOf(MtrHopStats(hopNumber = 1, ipAddress = "host", hostName = "host", sent = 5, received = 5, lastRttMs = 1.0, bestRttMs = 1.0, worstRttMs = 1.0, totalRttMs = 5.0))
        repository.updateWinMtrStats(stats)
        assertEquals(1, repository.winMtrStats.value.size)

        repository.clearActiveTrace()
        assertNull(repository.activeTraceType.value)
        assertNull(repository.activeTarget.value)
    }
}
