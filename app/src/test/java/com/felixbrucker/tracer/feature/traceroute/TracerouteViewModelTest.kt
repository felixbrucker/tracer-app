package com.felixbrucker.tracer.feature.traceroute

import com.felixbrucker.tracer.core.data.FavoriteRepository
import com.felixbrucker.tracer.core.data.NetworkToolRepository
import com.felixbrucker.tracer.core.datastore.SettingsRepository
import com.felixbrucker.tracer.core.model.TraceSettings
import com.felixbrucker.tracer.core.model.TracerouteHop
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TracerouteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val networkToolRepository = mockk<NetworkToolRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val favoriteRepository = mockk<FavoriteRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { settingsRepository.settingsFlow } returns flowOf(TraceSettings())
        coEvery { favoriteRepository.favorites } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStartTracerouteCollectsHops() = runTest {
        val hops = listOf(
            TracerouteHop(1, "gateway", "192.168.1.1", 2f, 2f, 2f, false),
            TracerouteHop(2, "isp", "10.0.0.1", 10f, 12f, 11f, true)
        )
        coEvery { networkToolRepository.executeTracerouteStream("1.1.1.1", any()) } returns flowOf(hops[0], hops[1])

        val viewModel = TracerouteViewModel(networkToolRepository, settingsRepository, favoriteRepository)
        viewModel.onHostChanged("1.1.1.1")
        viewModel.startTraceroute()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.hops.size)
        assertEquals("gateway", viewModel.uiState.value.hops[0].host)
        assertFalse(viewModel.uiState.value.isRunning)
    }

    @Test
    fun testStopTracerouteCancels() = runTest {
        val viewModel = TracerouteViewModel(networkToolRepository, settingsRepository, favoriteRepository)

        viewModel.stopTraceroute()

        assertFalse(viewModel.uiState.value.isRunning)
    }
}
