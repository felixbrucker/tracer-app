package com.felixbrucker.tracer.core.data

import com.felixbrucker.tracer.core.database.FavoriteDao
import com.felixbrucker.tracer.core.database.FavoriteEntity
import com.felixbrucker.tracer.core.model.FavoriteTarget
import com.felixbrucker.tracer.core.model.TraceMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteRepositoryTest {

    @Test
    fun testGetFavoritesTransformsEntities() = runTest {
        val daoMock = mockk<FavoriteDao>(relaxed = true)
        val entityList = listOf(
            FavoriteEntity(1, "Google", "8.8.8.8", "PING", 1000L)
        )
        coEvery { daoMock.getAllFavorites() } returns flowOf(entityList)

        val repository = FavoriteRepository(daoMock)
        val result = repository.favorites.first()

        assertEquals(1, result.size)
        assertEquals("Google", result[0].name)
        assertEquals("8.8.8.8", result[0].host)
        assertEquals(TraceMode.PING, result[0].defaultMode)
    }

    @Test
    fun testAddFavoriteCallsDao() = runTest {
        val daoMock = mockk<FavoriteDao>(relaxed = true)
        val target = FavoriteTarget(0, "Cloudflare", "1.1.1.1", TraceMode.LIVE_MTR, 2000L)
        coEvery { daoMock.insertFavorite(any()) } returns 1L

        val repository = FavoriteRepository(daoMock)
        val insertedId = repository.addFavorite(target)

        assertEquals(1L, insertedId)
        coVerify { daoMock.insertFavorite(any()) }
    }

    @Test
    fun testRemoveFavoriteCallsDao() = runTest {
        val daoMock = mockk<FavoriteDao>(relaxed = true)
        val target = FavoriteTarget(1, "Cloudflare", "1.1.1.1", TraceMode.LIVE_MTR, 2000L)
        coEvery { daoMock.deleteFavorite(any()) } returns Unit

        val repository = FavoriteRepository(daoMock)
        repository.removeFavorite(target)

        coVerify { daoMock.deleteFavorite(any()) }
    }
}
