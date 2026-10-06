package com.felixbrucker.tracer.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.felixbrucker.tracer.core.model.FavoriteTarget
import com.felixbrucker.tracer.core.model.TraceMode

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val defaultMode: String,
    val createdAtMs: Long
) {
    fun toDomain(): FavoriteTarget = FavoriteTarget(
        id = id,
        name = name,
        host = host,
        defaultMode = runCatching { TraceMode.valueOf(defaultMode) }.getOrDefault(TraceMode.LIVE_MTR),
        createdAtMs = createdAtMs
    )

    companion object {
        fun fromDomain(domain: FavoriteTarget): FavoriteEntity = FavoriteEntity(
            id = domain.id,
            name = domain.name,
            host = domain.host,
            defaultMode = domain.defaultMode.name,
            createdAtMs = domain.createdAtMs
        )
    }
}
