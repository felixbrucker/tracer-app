package com.felixbrucker.tracer.core.model

import javax.annotation.concurrent.Immutable

@Immutable
data class PingResult(
    val sequence: Int,
    val bytes: Int = 64,
    val ip: String,
    val ttl: Int = 64,
    val rttMs: Float? = null,
    val isSuccess: Boolean = true,
    val timestampMs: Long = System.currentTimeMillis()
)

@Immutable
data class PingSummary(
    val transmitted: Int = 0,
    val received: Int = 0,
    val minRttMs: Float? = null,
    val avgRttMs: Float? = null,
    val maxRttMs: Float? = null,
    val stdevRttMs: Float? = null
) {
    val lossPercentage: Float
        get() = if (transmitted > 0) ((transmitted - received).toFloat() / transmitted.toFloat()) * 100f else 0f
}
