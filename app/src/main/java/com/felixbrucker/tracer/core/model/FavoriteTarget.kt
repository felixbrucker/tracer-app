package com.felixbrucker.tracer.core.model

import javax.annotation.concurrent.Immutable

@Immutable
data class FavoriteTarget(
    val id: Long = 0,
    val name: String,
    val host: String,
    val defaultMode: TraceMode = TraceMode.LIVE_MTR,
    val createdAtMs: Long = System.currentTimeMillis()
)
