package com.felixbrucker.tracer.feature.traceroute

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felixbrucker.tracer.core.model.HopResult

@Composable
fun TracerouteScreenContent(
    hops: List<HopResult>,
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

    LaunchedEffect(hops.size) {
        if (hops.isNotEmpty() && isAtBottom) {
            listState.animateScrollToItem(hops.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        items(hops, key = { it.hopNumber }) { hop ->
            HopItemCard(hop = hop)
        }
    }
}

@Composable
fun HopItemCard(hop: HopResult) {
    val rtt = hop.rttMs
    val rttColor = when {
        hop.isTimeout || rtt == null -> MaterialTheme.colorScheme.error
        rtt < 50.0 -> Color(0xFF2E7D32)
        rtt < 150.0 -> Color(0xFFF57F17)
        else -> Color(0xFFD84315)
    }

    val displayHostName = hop.hostName
    val hasReverseHost = !displayHostName.isNullOrBlank() && displayHostName != "*" && displayHostName != hop.ipAddress

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                hop.isFinalDestination -> MaterialTheme.colorScheme.primaryContainer
                hop.isTimeout -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hop ${hop.hopNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (hasReverseHost && displayHostName != null) {
                    Text(
                        text = displayHostName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = hop.ipAddress ?: "*",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = hop.ipAddress ?: "*",
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (hop.isFinalDestination) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Text(
                text = if (hop.isTimeout) "*" else "${"%.1f".format(rtt ?: 0.0)} ms",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = rttColor
            )
        }
    }
}
