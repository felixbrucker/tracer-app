package com.felixbrucker.tracer.core.model

import javax.annotation.concurrent.Immutable

@Immutable
data class TracerouteHop(
    val hopNumber: Int,
    val host: String,
    val ip: String? = null,
    val rtt1Ms: Float? = null,
    val rtt2Ms: Float? = null,
    val rtt3Ms: Float? = null,
    val isReached: Boolean = false
)
