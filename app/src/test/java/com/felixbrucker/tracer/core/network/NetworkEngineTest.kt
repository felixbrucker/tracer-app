package com.felixbrucker.tracer.core.network

import com.felixbrucker.tracer.core.datastore.TracerSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class NetworkEngineTest {

    private val processMock = mockk<Process>(relaxed = true)
    private lateinit var engine: NetworkEngine

    @Before
    fun setUp() {
        engine = spyk(NetworkEngine())
        every { engine.createProcess(*varargAll { true }) } returns processMock
        every { processMock.inputStream } returns ByteArrayInputStream("64 bytes from 8.8.8.8: icmp_seq=1 ttl=54 time=12.5 ms\n".toByteArray())
        every { processMock.waitFor(any(), any()) } returns true
        every { processMock.exitValue() } returns 0
    }

    @Test
    fun testExecutePingStreamWithMockedProcess() = runTest {
        val settings = TracerSettings(pingIntervalMs = 5L, pingTimeoutSec = 1)
        val results = engine.executePingStream("8.8.8.8", count = 1, settings = settings).toList()
        assertNotNull(results)
        assertTrue(results.first().isSuccess)
    }

    @Test
    fun testExecutePingStreamWindowsFormat() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("Reply from 8.8.8.8: bytes=32 time=12ms TTL=54\n".toByteArray())

        val settings = TracerSettings(pingIntervalMs = 5L, pingTimeoutSec = 1)
        val results = engine.executePingStream("8.8.8.8", count = 1, settings = settings).toList()
        assertNotNull(results)
        assertTrue(results.first().isSuccess)
    }

    @Test
    fun testExecutePingStreamWithHostName() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("64 bytes from dns.google (8.8.8.8): icmp_seq=1 ttl=54 time=10.0 ms\n".toByteArray())

        val settings = TracerSettings(pingIntervalMs = 5L, pingTimeoutSec = 1)
        val results = engine.executePingStream("8.8.8.8", count = 1, settings = settings).toList()
        assertNotNull(results)
        assertTrue(results.first().isSuccess)
    }

    @Test
    fun testExecutePingStreamTimeout() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("Request timed out.\n".toByteArray())
        every { processMock.exitValue() } returns 1

        val settings = TracerSettings(pingIntervalMs = 5L, pingTimeoutSec = 1)
        val results = engine.executePingStream("8.8.8.8", count = 1, settings = settings).toList()
        assertNotNull(results)
        assertFalse(results.first().isSuccess)
    }

    @Test
    fun testExecutePingStreamProcessTimeoutUnfinished() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("".toByteArray())
        every { processMock.waitFor(any(), any()) } returns false

        val settings = TracerSettings(pingIntervalMs = 5L, pingTimeoutSec = 1)
        val results = engine.executePingStream("8.8.8.8", count = 1, settings = settings).toList()
        assertNotNull(results)
        assertFalse(results.first().isSuccess)
    }

    @Test
    fun testExecutePingStreamException() = runTest {
        every { engine.createProcess(*varargAll { true }) } throws RuntimeException("Exec failed")

        val settings = TracerSettings(pingIntervalMs = 5L, pingTimeoutSec = 1)
        val results = engine.executePingStream("8.8.8.8", count = 1, settings = settings).toList()
        assertNotNull(results)
        assertFalse(results.first().isSuccess)
    }

    @Test
    fun testExecuteTracerouteStreamWithMockedProcess() = runTest {
        val settings = TracerSettings(maxHops = 2, pingTimeoutSec = 1)
        val results = engine.executeTracerouteStream("8.8.8.8", settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteTracerouteStreamMultipleHopsSameIp() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("64 bytes from dns.google (8.8.8.8): icmp_seq=1 ttl=54 time=10.0 ms\n".toByteArray())

        val settings = TracerSettings(maxHops = 3, pingTimeoutSec = 1)
        val results = engine.executeTracerouteStream("8.8.8.8", settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteTracerouteStreamTtlExceeded() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("From 192.168.1.1: Time to live exceeded\n".toByteArray())

        val settings = TracerSettings(maxHops = 2, pingTimeoutSec = 1)
        val results = engine.executeTracerouteStream("8.8.8.8", settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteTracerouteStreamException() = runTest {
        every { engine.createProcess(*varargAll { true }) } throws RuntimeException("Exec failed")

        val settings = TracerSettings(maxHops = 2, pingTimeoutSec = 1)
        val results = engine.executeTracerouteStream("8.8.8.8", settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteMtrStreamWithMockedProcess() = runTest {
        val settings = TracerSettings(maxHops = 2, pingTimeoutSec = 1)
        val results = engine.executeMtrStream("8.8.8.8", settings = settings).take(1).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteTracerouteStreamParallelHopsWithFinalDestination() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("64 bytes from 8.8.8.8: icmp_seq=1 ttl=54 time=10.0 ms\n".toByteArray())

        val settings = TracerSettings(maxHops = 10, pingTimeoutSec = 1)
        val results = engine.executeTracerouteStream("8.8.8.8", settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteMtrStreamMultipleCycles() = runTest {
        val settings = TracerSettings(maxHops = 5, pingIntervalMs = 1L, pingTimeoutSec = 1)
        val results = engine.executeMtrStream("8.8.8.8", settings = settings).take(2).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteMtrStreamException() = runTest {
        every { engine.createProcess(*varargAll { true }) } throws RuntimeException("Exec failed")

        val settings = TracerSettings(maxHops = 2, pingTimeoutSec = 1)
        val results = engine.executeMtrStream("8.8.8.8", settings = settings).take(1).toList()
        assertNotNull(results)
    }
}
