package com.felixbrucker.tracer.core.datastore

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun testTracerSettingsDefaults() {
        val settings = TracerSettings(pingIntervalMs = 2000L, pingTimeoutSec = 5, maxHops = 20)
        assertEquals(2000L, settings.pingIntervalMs)
        assertEquals(5, settings.pingTimeoutSec)
        assertEquals(20, settings.maxHops)

        val defaultSettings = TracerSettings()
        assertEquals(1000L, defaultSettings.pingIntervalMs)
        assertEquals(2, defaultSettings.pingTimeoutSec)
        assertEquals(30, defaultSettings.maxHops)
    }

    @Test
    fun testAppSettingsRepositoryCreation() = testScope.runTest {
        val contextMock = mockk<Context>(relaxed = true)
        val repository = AppSettingsRepository(contextMock)
        assertNotNull(repository)
        assertNotNull(repository.tracerSettings)
    }
}
