package com.felixbrucker.tracer.feature.ping

import com.felixbrucker.tracer.core.data.FavoriteRepository
import com.felixbrucker.tracer.core.data.NetworkToolRepository
import com.felixbrucker.tracer.core.datastore.SettingsRepository
import com.felixbrucker.tracer.core.model.PingResult
import com.felixbrucker.tracer.core.model.TraceSettings
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
class PingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val networkToolRepository = mockk<NetworkToolRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val favoriteRepository = mockk<FavoriteRepository>()

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
    fun testStartPingCollectsResultsAndUpdatesSummary() = runTest {
        val pingResults = listOf(
            PingResult(1, 64, "8.8.8.8", 64, 10f, true),
            PingResult(2, 64, "8.8.8.8", 64, 20f, true)
        )
        coEvery { networkToolRepository.executePingStream("8.8.8.8", any()) } returns flowOf(pingResults[0], pingResults[1])

        val viewModel = PingViewModel(networkToolRepository, settingsRepository, favoriteRepository)
        viewModel.onHostChanged("8.8.8.8")
        viewModel.startPing()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.results.size)
        assertEquals(2, viewModel.uiState.value.summary.transmitted)
        assertEquals(2, viewModel.uiState.value.summary.received)
        assertEquals(15f, viewModel.uiState.value.summary.avgRttMs)
    }

    @Test
    fun testStopPingCancelsJob() = runTest {
        val viewModel = PingViewModel(networkToolRepository, settingsRepository, favoriteRepository)

        viewModel.stopPing()

        assertFalse(viewModel.uiState.value.isRunning)
    }
}
