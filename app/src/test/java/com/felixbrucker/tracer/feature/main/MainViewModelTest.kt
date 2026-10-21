package com.felixbrucker.tracer.feature.main

import com.felixbrucker.tracer.core.data.FavoriteRepository
import com.felixbrucker.tracer.core.data.TraceRepository
import com.felixbrucker.tracer.core.datastore.AppSettingsRepository
import com.felixbrucker.tracer.core.datastore.TracerSettings
import com.felixbrucker.tracer.core.database.entity.FavoriteTargetEntity
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAddFavoriteInvokesRepository() = runTest {
        val favoriteRepoMock = mockk<FavoriteRepository>(relaxed = true)
        val traceRepoMock = mockk<TraceRepository>(relaxed = true)
        val settingsRepoMock = mockk<AppSettingsRepository>(relaxed = true)
        coEvery { favoriteRepoMock.favorites } returns flowOf(emptyList())
        coEvery { settingsRepoMock.tracerSettings } returns flowOf(TracerSettings())
        coEvery { favoriteRepoMock.addFavorite("Test", "127.0.0.1") } returns 1L
        val viewModel = MainViewModel(traceRepoMock, favoriteRepoMock, settingsRepoMock)

        viewModel.addFavorite("Test", "127.0.0.1")
        advanceUntilIdle()

        coVerify { favoriteRepoMock.addFavorite("Test", "127.0.0.1") }
    }

    @Test
    fun testUpdateFavoriteInvokesRepository() = runTest {
        val favoriteRepoMock = mockk<FavoriteRepository>(relaxed = true)
        val traceRepoMock = mockk<TraceRepository>(relaxed = true)
        val settingsRepoMock = mockk<AppSettingsRepository>(relaxed = true)
        coEvery { favoriteRepoMock.favorites } returns flowOf(emptyList())
        coEvery { settingsRepoMock.tracerSettings } returns flowOf(TracerSettings())
        val viewModel = MainViewModel(traceRepoMock, favoriteRepoMock, settingsRepoMock)

        viewModel.updateFavorite(1L, "Updated", "1.1.1.1")
        advanceUntilIdle()

        coVerify { favoriteRepoMock.updateFavorite(1L, "Updated", "1.1.1.1") }
    }

    @Test
    fun testRemoveFavoriteInvokesRepository() = runTest {
        val favoriteRepoMock = mockk<FavoriteRepository>(relaxed = true)
        val traceRepoMock = mockk<TraceRepository>(relaxed = true)
        val settingsRepoMock = mockk<AppSettingsRepository>(relaxed = true)
        coEvery { favoriteRepoMock.favorites } returns flowOf(emptyList())
        coEvery { settingsRepoMock.tracerSettings } returns flowOf(TracerSettings())
        coEvery { favoriteRepoMock.removeFavorite(5L) } returns Unit
        val viewModel = MainViewModel(traceRepoMock, favoriteRepoMock, settingsRepoMock)

        viewModel.removeFavorite(5L)
        advanceUntilIdle()

        coVerify { favoriteRepoMock.removeFavorite(5L) }
    }

    @Test
    fun testUpdateSettingsInvokesRepository() = runTest {
        val favoriteRepoMock = mockk<FavoriteRepository>(relaxed = true)
        val traceRepoMock = mockk<TraceRepository>(relaxed = true)
        val settingsRepoMock = mockk<AppSettingsRepository>(relaxed = true)
        coEvery { favoriteRepoMock.favorites } returns flowOf(emptyList())
        coEvery { settingsRepoMock.tracerSettings } returns flowOf(TracerSettings())
        val viewModel = MainViewModel(traceRepoMock, favoriteRepoMock, settingsRepoMock)

        viewModel.updatePingIntervalMs(2000L)
        viewModel.updatePingTimeoutSec(5)
        viewModel.updateMaxHops(25)
        viewModel.updateLastUsedHost("8.8.8.8")
        advanceUntilIdle()

        coVerify { settingsRepoMock.updatePingIntervalMs(2000L) }
        coVerify { settingsRepoMock.updatePingTimeoutSec(5) }
        coVerify { settingsRepoMock.updateMaxHops(25) }
        coVerify { settingsRepoMock.updateLastUsedHost("8.8.8.8") }
    }

    @Test
    fun testViewModelStateFlows() = runTest {
        val favoriteRepoMock = mockk<FavoriteRepository>(relaxed = true)
        val traceRepoMock = mockk<TraceRepository>(relaxed = true)
        val settingsRepoMock = mockk<AppSettingsRepository>(relaxed = true)
        coEvery { favoriteRepoMock.favorites } returns flowOf(emptyList())
        coEvery { settingsRepoMock.tracerSettings } returns flowOf(TracerSettings())

        val viewModel = MainViewModel(traceRepoMock, favoriteRepoMock, settingsRepoMock)
        assertEquals(emptyList<FavoriteTargetEntity>(), viewModel.favorites.value)
        assertEquals(TracerSettings(), viewModel.tracerSettings.value)
        assertNotNull(viewModel.pingResults)
        assertNotNull(viewModel.tracerouteHops)
        assertNotNull(viewModel.mtrStats)
        assertNotNull(viewModel.activeTraceType)
        assertNotNull(viewModel.activeTarget)
    }
}
