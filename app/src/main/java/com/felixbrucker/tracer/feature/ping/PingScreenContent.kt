package com.felixbrucker.tracer.feature.ping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felixbrucker.tracer.core.model.PingResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun PingScreenContent(
    results: List<PingResult>,
    isRunning: Boolean,
    targetHost: String,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) true
            else {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem != null && lastVisibleItem.index >= totalItems - 2
            }
        }
    }

    LaunchedEffect(results.size) {
        if (results.isNotEmpty() && isAtBottom) {
            listState.animateScrollToItem(results.size)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        if (results.isNotEmpty()) {
            item {
                PingSummaryCard(results = results, targetHost = targetHost, isRunning = isRunning)
            }
        }
        items(results, key = { it.sequence }) { ping ->
            PingItemCard(ping = ping)
        }
    }
}

@Composable
fun PingSummaryCard(results: List<PingResult>, targetHost: String, isRunning: Boolean) {
    val total = results.size
    val successful = results.count { it.isSuccess }
    val failed = total - successful
    val lossPct = if (total == 0) 0.0 else (failed.toDouble() / total.toDouble()) * 100.0

    val rtts = results.mapNotNull { it.rttMs }
    val minRtt = rtts.minOrNull() ?: 0.0
    val maxRtt = rtts.maxOrNull() ?: 0.0
    val avgRtt = if (rtts.isNotEmpty()) rtts.average() else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Ping Statistics for $targetHost ${if (isRunning) "(Active)" else "(Finished)"}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Text(
                    text = "Packets: Sent = $total, Received = $successful, Lost = $failed (${"%.0f".format(lossPct)}% loss)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (rtts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    Text(
                        text = "RTT Min = ${"%.0f".format(minRtt)}ms | Avg = ${"%.0f".format(avgRtt)}ms | Max = ${"%.0f".format(maxRtt)}ms",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun PingItemCard(ping: PingResult) {
    val timeFormatted = rememberFormattedTime(ping.timestamp)

    val rttColor = when {
        !ping.isSuccess -> MaterialTheme.colorScheme.error
        ping.rttMs == null -> MaterialTheme.colorScheme.onSurface
        ping.rttMs < 50.0 -> Color(0xFF2E7D32)
        ping.rttMs < 150.0 -> Color(0xFFFBC02D)
        ping.rttMs < 300.0 -> Color(0xFFFF9800)
        ping.rttMs < 500.0 -> Color(0xFFF57C00)
        else -> Color(0xFFD32F2F)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ping.isSuccess) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${ping.sequence}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = timeFormatted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = "IP: ${ping.ipAddress ?: ping.target}",
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "TTL: ${ping.ttl}",
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (ping.isSuccess && ping.rttMs != null) {
                    Text(
                        text = "${"%.0f".format(ping.rttMs)} ms",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = rttColor,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = "Timed out",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberFormattedTime(timestamp: Long): String {
    val locale = LocalLocale.current.platformLocale
    return remember(timestamp, locale) {
        val sdf = SimpleDateFormat("HH:mm:ss", locale)
        sdf.format(Date(timestamp))
    }
}
