package com.felixbrucker.tracer.core.model

import javax.annotation.concurrent.Immutable

@Immutable
data class HopStats(
    val hopNumber: Int,
    val host: String,
    val ip: String? = null,
    val sent: Int = 0,
    val received: Int = 0,
    val lastRttMs: Float? = null,
    val bestRttMs: Float? = null,
    val worstRttMs: Float? = null,
    val sumRttMs: Double = 0.0,
    val sumSqRttMs: Double = 0.0,
    val historyRttMs: List<Float?> = emptyList()
) {
    val lossPercentage: Float
        get() = if (sent > 0) ((sent - received).toFloat() / sent.toFloat()) * 100f else 0f

    val avgRttMs: Float?
        get() = if (received > 0) (sumRttMs / received).toFloat() else null

    val stDevRttMs: Float?
        get() {
            if (received <= 1) return 0f
            val avg = sumRttMs / received
            val variance = (sumSqRttMs / received) - (avg * avg)
            return if (variance > 0) kotlin.math.sqrt(variance).toFloat() else 0f
        }
}
