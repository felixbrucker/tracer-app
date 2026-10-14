package com.felixbrucker.tracer.core.data

import com.felixbrucker.tracer.core.database.dao.FavoriteTargetDao
import com.felixbrucker.tracer.core.database.entity.FavoriteTargetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteTargetDao: FavoriteTargetDao
) {
    val favorites: Flow<List<FavoriteTargetEntity>> = favoriteTargetDao.getAllFavorites()

    suspend fun addFavorite(title: String?, host: String): Long {
        val entity = FavoriteTargetEntity(
            title = if (title.isNullOrBlank()) null else title,
            host = host
        )
        return favoriteTargetDao.insertFavorite(entity)
    }

    suspend fun updateFavorite(id: Long, title: String?, host: String) {
        val entity = FavoriteTargetEntity(
            id = id,
            title = if (title.isNullOrBlank()) null else title,
            host = host
        )
        favoriteTargetDao.updateFavorite(entity)
    }

    suspend fun removeFavorite(id: Long) {
        favoriteTargetDao.deleteFavoriteById(id)
    }
}
