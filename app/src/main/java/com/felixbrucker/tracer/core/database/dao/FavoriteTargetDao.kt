package com.felixbrucker.tracer.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.felixbrucker.tracer.core.database.entity.FavoriteTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteTargetDao {
    @Query("SELECT * FROM favorite_targets ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteTargetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteTargetEntity): Long

    @Update
    suspend fun updateFavorite(favorite: FavoriteTargetEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteTargetEntity)

    @Query("DELETE FROM favorite_targets WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)
}
