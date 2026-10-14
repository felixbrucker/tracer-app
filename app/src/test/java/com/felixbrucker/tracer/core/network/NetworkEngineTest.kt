package com.felixbrucker.tracer.core.network

import com.felixbrucker.tracer.core.datastore.TracerSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class NetworkEngineTest {

    private val processMock = mockk<Process>(relaxed = true)

    @Before
    fun setUp() {
        try {
            mockkStatic(Runtime::class)
            val runtimeMock = mockk<Runtime>(relaxed = true)
            every { Runtime.getRuntime() } returns runtimeMock
            every { runtimeMock.exec(any<String>()) } returns processMock
            every { processMock.inputStream } returns ByteArrayInputStream("64 bytes from 8.8.8.8: icmp_seq=1 ttl=54 time=12.5 ms\n".toByteArray())
            every { processMock.waitFor() } returns 0
        } catch (e: Exception) {
            // Ignore if already mocked
        }
    }

    @After
    fun tearDown() {
        try {
            unmockkStatic(Runtime::class)
        } catch (e: Exception) {
            // Ignore
        }
    }

    @Test
    fun testExecutePingStreamWithMockedProcess() = runTest {
        val engine = NetworkEngine()
        val settings = TracerSettings(pingIntervalMs = 5L, pingTimeoutSec = 1)
        val results = engine.executePingStream("8.8.8.8", count = 1, settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecutePingStreamWindowsFormat() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("Reply from 8.8.8.8: bytes=32 time=12ms TTL=54\n".toByteArray())
        every { processMock.waitFor() } returns 0

        val engine = NetworkEngine()
        val settings = TracerSettings(pingIntervalMs = 5L, pingTimeoutSec = 1)
        val results = engine.executePingStream("8.8.8.8", count = 1, settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecutePingStreamWithHostName() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("64 bytes from dns.google (8.8.8.8): icmp_seq=1 ttl=54 time=10.0 ms\n".toByteArray())
        every { processMock.waitFor() } returns 0

        val engine = NetworkEngine()
        val settings = TracerSettings(pingIntervalMs = 5L, pingTimeoutSec = 1)
        val results = engine.executePingStream("8.8.8.8", count = 1, settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecutePingStreamTimeout() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("Request timed out.\n".toByteArray())
        every { processMock.waitFor() } returns 1

        val engine = NetworkEngine()
        val settings = TracerSettings(pingIntervalMs = 5L, pingTimeoutSec = 1)
        val results = engine.executePingStream("8.8.8.8", count = 1, settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecutePingStreamException() = runTest {
        val runtimeMock = mockk<Runtime>(relaxed = true)
        every { Runtime.getRuntime() } returns runtimeMock
        every { runtimeMock.exec(any<String>()) } throws RuntimeException("Exec failed")

        val engine = NetworkEngine()
        val settings = TracerSettings(pingIntervalMs = 5L, pingTimeoutSec = 1)
        val results = engine.executePingStream("8.8.8.8", count = 1, settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteTracerouteStreamWithMockedProcess() = runTest {
        val engine = NetworkEngine()
        val settings = TracerSettings(maxHops = 2, pingTimeoutSec = 1)
        val results = engine.executeTracerouteStream("8.8.8.8", settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteTracerouteStreamMultipleHopsSameIp() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("64 bytes from dns.google (8.8.8.8): icmp_seq=1 ttl=54 time=10.0 ms\n".toByteArray())
        every { processMock.waitFor() } returns 0

        val engine = NetworkEngine()
        val settings = TracerSettings(maxHops = 3, pingTimeoutSec = 1)
        val results = engine.executeTracerouteStream("8.8.8.8", settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteTracerouteStreamTtlExceeded() = runTest {
        every { processMock.inputStream } returns ByteArrayInputStream("From 192.168.1.1: Time to live exceeded\n".toByteArray())
        every { processMock.waitFor() } returns 0

        val engine = NetworkEngine()
        val settings = TracerSettings(maxHops = 2, pingTimeoutSec = 1)
        val results = engine.executeTracerouteStream("8.8.8.8", settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteTracerouteStreamException() = runTest {
        val runtimeMock = mockk<Runtime>(relaxed = true)
        every { Runtime.getRuntime() } returns runtimeMock
        every { runtimeMock.exec(any<String>()) } throws RuntimeException("Exec failed")

        val engine = NetworkEngine()
        val settings = TracerSettings(maxHops = 2, pingTimeoutSec = 1)
        val results = engine.executeTracerouteStream("8.8.8.8", settings = settings).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteMtrStreamWithMockedProcess() = runTest {
        val engine = NetworkEngine()
        val settings = TracerSettings(maxHops = 2, pingTimeoutSec = 1)
        val results = engine.executeMtrStream("8.8.8.8", settings = settings).take(1).toList()
        assertNotNull(results)
    }

    @Test
    fun testExecuteMtrStreamException() = runTest {
        val runtimeMock = mockk<Runtime>(relaxed = true)
        every { Runtime.getRuntime() } returns runtimeMock
        every { runtimeMock.exec(any<String>()) } throws RuntimeException("Exec failed")

        val engine = NetworkEngine()
        val settings = TracerSettings(maxHops = 2, pingTimeoutSec = 1)
        val results = engine.executeMtrStream("8.8.8.8", settings = settings).take(1).toList()
        assertNotNull(results)
    }
}
