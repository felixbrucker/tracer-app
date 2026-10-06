package com.felixbrucker.tracer.feature.favorites

import com.felixbrucker.tracer.core.data.FavoriteRepository
import com.felixbrucker.tracer.core.model.FavoriteTarget
import com.felixbrucker.tracer.core.model.TraceMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val favoriteRepository = mockk<FavoriteRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { favoriteRepository.favorites } returns flowOf(
            listOf(
                FavoriteTarget(1, "Google", "8.8.8.8", TraceMode.PING, 1000L)
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFavoritesStateFlow() = runTest {
        val viewModel = FavoritesViewModel(favoriteRepository)

        val collectJob = launch { viewModel.favorites.collect() }
        advanceUntilIdle()

        assertEquals(1, viewModel.favorites.value.size)
        assertEquals("Google", viewModel.favorites.value[0].name)
        collectJob.cancel()
    }

    @Test
    fun testAddFavoriteCallsRepository() = runTest {
        val viewModel = FavoritesViewModel(favoriteRepository)

        viewModel.addFavorite("Cloudflare", "1.1.1.1", TraceMode.LIVE_MTR)
        advanceUntilIdle()

        coVerify { favoriteRepository.addFavorite(any()) }
    }

    @Test
    fun testRemoveFavoriteCallsRepository() = runTest {
        val target = FavoriteTarget(1, "Google", "8.8.8.8", TraceMode.PING, 1000L)
        val viewModel = FavoritesViewModel(favoriteRepository)

        viewModel.removeFavorite(target)
        advanceUntilIdle()

        coVerify { favoriteRepository.removeFavorite(target) }
    }
}
