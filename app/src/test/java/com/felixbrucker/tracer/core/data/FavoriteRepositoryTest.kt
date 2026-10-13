package com.felixbrucker.tracer.core.data

import com.felixbrucker.tracer.core.database.dao.FavoriteTargetDao
import com.felixbrucker.tracer.core.database.entity.FavoriteTargetEntity
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
    fun testGetFavoritesSuccess() = runTest {
        val daoMock = mockk<FavoriteTargetDao>(relaxed = true)
        val expectedList = listOf(FavoriteTargetEntity(id = 1L, title = "Google DNS", host = "8.8.8.8", defaultType = "PING"))
        coEvery { daoMock.getAllFavorites() } returns flowOf(expectedList)
        val repository = FavoriteRepository(daoMock)

        val result = repository.favorites.first()

        assertEquals(expectedList, result)
    }

    @Test
    fun testAddFavoriteSuccess() = runTest {
        val daoMock = mockk<FavoriteTargetDao>(relaxed = true)
        coEvery { daoMock.insertFavorite(any()) } returns 1L
        val repository = FavoriteRepository(daoMock)

        val id = repository.addFavorite("Cloudflare", "1.1.1.1", "WINMTR")

        assertEquals(1L, id)
        coVerify { daoMock.insertFavorite(match { it.title == "Cloudflare" && it.host == "1.1.1.1" }) }
    }

    @Test
    fun testRemoveFavoriteSuccess() = runTest {
        val daoMock = mockk<FavoriteTargetDao>(relaxed = true)
        coEvery { daoMock.deleteFavoriteById(1L) } returns Unit
        val repository = FavoriteRepository(daoMock)

        repository.removeFavorite(1L)

        coVerify { daoMock.deleteFavoriteById(1L) }
    }
}
