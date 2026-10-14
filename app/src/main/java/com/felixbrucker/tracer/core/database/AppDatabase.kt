package com.felixbrucker.tracer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.felixbrucker.tracer.core.database.dao.FavoriteTargetDao
import com.felixbrucker.tracer.core.database.entity.FavoriteTargetEntity

@Database(
    entities = [FavoriteTargetEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteTargetDao(): FavoriteTargetDao
}
