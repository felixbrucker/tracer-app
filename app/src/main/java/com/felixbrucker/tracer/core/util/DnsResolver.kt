package com.felixbrucker.tracer.core.util

import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsResolver @Inject constructor() {
    suspend fun resolveHost(host: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            InetAddress.getByName(host).hostAddress
        }.getOrNull()
    }

    suspend fun resolveReverse(ip: String): String = withContext(Dispatchers.IO) {
        runCatching {
            InetAddress.getByName(ip).canonicalHostName
        }.getOrDefault(ip)
    }
}
