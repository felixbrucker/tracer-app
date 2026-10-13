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

    suspend fun addFavorite(title: String, host: String, defaultType: String): Long {
        val entity = FavoriteTargetEntity(
            title = title,
            host = host,
            defaultType = defaultType
        )
        return favoriteTargetDao.insertFavorite(entity)
    }

    suspend fun removeFavorite(id: Long) {
        favoriteTargetDao.deleteFavoriteById(id)
    }
}
