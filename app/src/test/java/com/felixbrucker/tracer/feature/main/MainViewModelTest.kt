package com.felixbrucker.tracer.feature.main

import com.felixbrucker.tracer.core.data.FavoriteRepository
import com.felixbrucker.tracer.core.data.TraceRepository
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
        coEvery { favoriteRepoMock.favorites } returns flowOf(emptyList())
        coEvery { favoriteRepoMock.addFavorite("Test", "127.0.0.1") } returns 1L
        val viewModel = MainViewModel(traceRepoMock, favoriteRepoMock)

        viewModel.addFavorite("Test", "127.0.0.1")
        advanceUntilIdle()

        coVerify { favoriteRepoMock.addFavorite("Test", "127.0.0.1") }
    }

    @Test
    fun testRemoveFavoriteInvokesRepository() = runTest {
        val favoriteRepoMock = mockk<FavoriteRepository>(relaxed = true)
        val traceRepoMock = mockk<TraceRepository>(relaxed = true)
        coEvery { favoriteRepoMock.favorites } returns flowOf(emptyList())
        coEvery { favoriteRepoMock.removeFavorite(5L) } returns Unit
        val viewModel = MainViewModel(traceRepoMock, favoriteRepoMock)

        viewModel.removeFavorite(5L)
        advanceUntilIdle()

        coVerify { favoriteRepoMock.removeFavorite(5L) }
    }
}
