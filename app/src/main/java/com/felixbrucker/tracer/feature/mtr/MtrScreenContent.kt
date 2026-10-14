package com.felixbrucker.tracer.feature.mtr

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felixbrucker.tracer.core.model.MtrHopStats

@Composable
fun MtrScreenContent(
    statsList: List<MtrHopStats>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems <= 1) true
            else {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem != null && lastVisibleItem.index >= totalItems - 2
            }
        }
    }

    LaunchedEffect(statsList.size) {
        if (statsList.isNotEmpty() && isAtBottom) {
            listState.animateScrollToItem(statsList.size)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 4.dp)
    ) {
        item {
            MtrHeaderRow()
        }
        items(statsList, key = { it.hopNumber }) { stat ->
            MtrHopCard(stat = stat)
        }
    }
}

@Composable
fun MtrHeaderRow() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", Modifier.width(22.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("IP / Host", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Loss%", Modifier.width(42.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("S/R", Modifier.width(46.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Avg", Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Best", Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Wrst", Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MtrHopCard(stat: MtrHopStats) {
    val cardColor by animateColorAsState(
        targetValue = when {
            stat.lossPercentage > 20.0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            stat.lossPercentage > 0.0 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "cardColorAnimation"
    )

    fun getRttColor(rtt: Double): Color = when {
        rtt == 0.0 -> Color.Unspecified
        rtt < 50.0 -> Color(0xFF2E7D32)
        rtt < 150.0 -> Color(0xFFFBC02D)
        rtt < 300.0 -> Color(0xFFFF9800)
        rtt < 500.0 -> Color(0xFFF57C00)
        else -> Color(0xFFD32F2F)
    }

    val displayHost = if (stat.hostName.isNotBlank() && stat.hostName != "*" && stat.hostName != stat.ipAddress) {
        "${stat.ipAddress} (${stat.hostName})"
    } else {
        stat.ipAddress
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stat.hopNumber}",
                modifier = Modifier.width(22.dp),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = displayHost,
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${"%.0f".format(stat.lossPercentage)}%",
                modifier = Modifier.width(42.dp),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (stat.lossPercentage > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${stat.sent}/${stat.received}",
                modifier = Modifier.width(46.dp),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "%.0f".format(stat.avgRttMs),
                modifier = Modifier.width(36.dp),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = getRttColor(stat.avgRttMs)
            )
            Text(
                text = if (stat.bestRttMs == Double.MAX_VALUE) "-" else "%.0f".format(stat.bestRttMs),
                modifier = Modifier.width(36.dp),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = getRttColor(if (stat.bestRttMs == Double.MAX_VALUE) 0.0 else stat.bestRttMs)
            )
            Text(
                text = "%.0f".format(stat.worstRttMs),
                modifier = Modifier.width(36.dp),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = getRttColor(stat.worstRttMs)
            )
        }
    }
}
