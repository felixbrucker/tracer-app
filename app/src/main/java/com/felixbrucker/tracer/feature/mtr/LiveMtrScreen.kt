package com.felixbrucker.tracer.feature.mtr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felixbrucker.tracer.core.model.HopStats

@Composable
fun LiveMtrScreen(
    uiState: LiveMtrUiState,
    onHostChanged: (String) -> Unit,
    onStartTrace: () -> Unit,
    onPauseTrace: () -> Unit,
    onResumeTrace: () -> Unit,
    onStopTrace: () -> Unit,
    onResetStats: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HostInputBar(
            host = uiState.targetHost,
            isFavorite = uiState.isFavorite,
            onHostChanged = onHostChanged,
            onToggleFavorite = onToggleFavorite
        )
        Spacer(modifier = Modifier.height(12.dp))
        MtrControlsBar(
            isTracing = uiState.isTracing,
            isPaused = uiState.isPaused,
            onStartTrace = onStartTrace,
            onPauseTrace = onPauseTrace,
            onResumeTrace = onResumeTrace,
            onStopTrace = onStopTrace,
            onResetStats = onResetStats
        )
        Spacer(modifier = Modifier.height(16.dp))
        MtrTableHeader()
        Spacer(modifier = Modifier.height(8.dp))
        MtrTableList(hops = uiState.hops)
    }
}

@Composable
private fun HostInputBar(
    host: String,
    isFavorite: Boolean,
    onHostChanged: (String) -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = host,
            onValueChange = onHostChanged,
            modifier = Modifier.weight(1f),
            label = { Text("Target Host or IP") },
            singleLine = true
        )
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Toggle Favorite",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MtrControlsBar(
    isTracing: Boolean,
    isPaused: Boolean,
    onStartTrace: () -> Unit,
    onPauseTrace: () -> Unit,
    onResumeTrace: () -> Unit,
    onStopTrace: () -> Unit,
    onResetStats: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isTracing) {
            Button(onClick = onStartTrace, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("Start MTR")
            }
        } else {
            if (isPaused) {
                Button(onClick = onResumeTrace, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("Resume")
                }
            } else {
                Button(onClick = onPauseTrace, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Pause, contentDescription = null)
                    Text("Pause")
                }
            }
            Button(onClick = onStopTrace, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Text("Stop")
            }
        }
        IconButton(onClick = onResetStats) {
            Icon(Icons.Filled.Refresh, contentDescription = "Reset Stats")
        }
    }
}

@Composable
private fun MtrTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("#", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Host", modifier = Modifier.weight(2.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Loss%", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("S/R", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Last", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Avg", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Best", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Wrst", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MtrTableList(hops: List<HopStats>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(hops, key = { it.hopNumber }) { hop ->
            HopRowCard(hop = hop)
        }
    }
}

@Composable
private fun HopRowCard(hop: HopStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(hop.hopNumber.toString(), modifier = Modifier.weight(0.6f), fontSize = 11.sp)
            Text(hop.host, modifier = Modifier.weight(2.5f), fontSize = 11.sp, maxLines = 1)
            Text(
                text = String.format("%.1f%%", hop.lossPercentage),
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                color = if (hop.lossPercentage > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text("${hop.sent}/${hop.received}", modifier = Modifier.weight(1f), fontSize = 11.sp)
            Text(formatRtt(hop.lastRttMs), modifier = Modifier.weight(1f), fontSize = 11.sp)
            Text(formatRtt(hop.avgRttMs), modifier = Modifier.weight(1f), fontSize = 11.sp)
            Text(formatRtt(hop.bestRttMs), modifier = Modifier.weight(1f), fontSize = 11.sp)
            Text(formatRtt(hop.worstRttMs), modifier = Modifier.weight(1f), fontSize = 11.sp)
        }
    }
}

private fun formatRtt(rtt: Float?): String {
    return rtt?.let { String.format("%.0fms", it) } ?: "*"
}
