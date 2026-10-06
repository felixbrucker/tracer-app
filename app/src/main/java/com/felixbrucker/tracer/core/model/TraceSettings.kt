package com.felixbrucker.tracer.core.model

import javax.annotation.concurrent.Immutable

@Immutable
data class TraceSettings(
    val maxHops: Int = 30,
    val packetSize: Int = 64,
    val timeoutMs: Int = 1000,
    val intervalMs: Long = 1000L,
    val resolveDns: Boolean = true
)
