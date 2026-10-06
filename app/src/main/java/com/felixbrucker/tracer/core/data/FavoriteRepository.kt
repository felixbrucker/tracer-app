package com.felixbrucker.tracer.core.data

import com.felixbrucker.tracer.core.database.FavoriteDao
import com.felixbrucker.tracer.core.database.FavoriteEntity
import com.felixbrucker.tracer.core.model.FavoriteTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao
) {
    val favorites: Flow<List<FavoriteTarget>> = favoriteDao.getAllFavorites().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun addFavorite(target: FavoriteTarget): Long {
        return favoriteDao.insertFavorite(FavoriteEntity.fromDomain(target))
    }

    suspend fun updateFavorite(target: FavoriteTarget) {
        favoriteDao.updateFavorite(FavoriteEntity.fromDomain(target))
    }

    suspend fun removeFavorite(target: FavoriteTarget) {
        favoriteDao.deleteFavorite(FavoriteEntity.fromDomain(target))
    }

    suspend fun removeFavoriteById(id: Long) {
        favoriteDao.deleteById(id)
    }
}
