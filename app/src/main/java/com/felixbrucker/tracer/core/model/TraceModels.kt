package com.felixbrucker.tracer.core.model

import androidx.compose.runtime.Immutable

enum class TraceType {
    PING,
    TRACEROUTE,
    MTR
}

@Immutable
data class PingResult(
    val sequence: Int,
    val target: String,
    val ipAddress: String?,
    val ttl: Int,
    val rttMs: Double?,
    val isSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class HopResult(
    val hopNumber: Int,
    val hostName: String?,
    val ipAddress: String?,
    val rttMs: Double?,
    val isFinalDestination: Boolean = false,
    val isTimeout: Boolean = false
)

@Immutable
data class MtrHopStats(
    val hopNumber: Int,
    val ipAddress: String = "*",
    val hostName: String = "*",
    val sent: Int = 0,
    val received: Int = 0,
    val lastRttMs: Double = 0.0,
    val bestRttMs: Double = Double.MAX_VALUE,
    val worstRttMs: Double = 0.0,
    val totalRttMs: Double = 0.0
) {
    val lossPercentage: Double
        get() = if (sent == 0) 0.0 else ((sent - received).toDouble() / sent.toDouble()) * 100.0

    val avgRttMs: Double
        get() = if (received == 0) 0.0 else totalRttMs / received
}
