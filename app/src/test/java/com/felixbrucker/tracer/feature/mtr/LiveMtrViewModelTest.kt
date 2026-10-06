package com.felixbrucker.tracer.feature.mtr

import com.felixbrucker.tracer.core.data.FavoriteRepository
import com.felixbrucker.tracer.core.data.NetworkToolRepository
import com.felixbrucker.tracer.core.datastore.SettingsRepository
import com.felixbrucker.tracer.core.model.FavoriteTarget
import com.felixbrucker.tracer.core.model.TraceMode
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveMtrViewModelTest {

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
    fun testOnHostChangedUpdatesState() = runTest {
        val viewModel = LiveMtrViewModel(networkToolRepository, settingsRepository, favoriteRepository)

        viewModel.onHostChanged("1.1.1.1")
        advanceUntilIdle()

        assertEquals("1.1.1.1", viewModel.uiState.value.targetHost)
    }

    @Test
    fun testToggleFavoriteAddsWhenNotFavorite() = runTest {
        coEvery { favoriteRepository.addFavorite(any()) } returns 1L
        val viewModel = LiveMtrViewModel(networkToolRepository, settingsRepository, favoriteRepository)

        viewModel.onHostChanged("8.8.8.8")
        advanceUntilIdle()
        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFavorite)
    }

    @Test
    fun testPauseAndResumeTrace() = runTest {
        val viewModel = LiveMtrViewModel(networkToolRepository, settingsRepository, favoriteRepository)

        viewModel.pauseTrace()

        assertTrue(viewModel.uiState.value.isPaused)

        viewModel.resumeTrace()

        assertFalse(viewModel.uiState.value.isPaused)
    }
}
