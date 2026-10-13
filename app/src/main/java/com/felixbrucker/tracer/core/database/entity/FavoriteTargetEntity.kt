package com.felixbrucker.tracer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_targets")
data class FavoriteTargetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val host: String,
    val defaultType: String,
    val addedAt: Long = System.currentTimeMillis()
)
