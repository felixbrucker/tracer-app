package com.felixbrucker.tracer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
