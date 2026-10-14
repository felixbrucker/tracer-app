package com.felixbrucker.tracer.di

import android.content.Context
import androidx.room.Room
import com.felixbrucker.tracer.core.database.AppDatabase
import com.felixbrucker.tracer.core.database.dao.FavoriteTargetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tracer.db"
        ).build()
    }

    @Provides
    fun provideFavoriteTargetDao(database: AppDatabase): FavoriteTargetDao {
        return database.favoriteTargetDao()
    }
}
