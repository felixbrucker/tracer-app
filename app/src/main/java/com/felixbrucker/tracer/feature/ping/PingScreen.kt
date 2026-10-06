package com.felixbrucker.tracer.feature.ping

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
import androidx.compose.material.icons.filled.PlayArrow
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
import com.felixbrucker.tracer.core.model.PingResult
import com.felixbrucker.tracer.core.model.PingSummary

@Composable
fun PingScreen(
    uiState: PingUiState,
    onHostChanged: (String) -> Unit,
    onStartPing: () -> Unit,
    onStopPing: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PingHostInput(
            host = uiState.targetHost,
            isFavorite = uiState.isFavorite,
            onHostChanged = onHostChanged,
            onToggleFavorite = onToggleFavorite
        )
        Spacer(modifier = Modifier.height(12.dp))
        PingActionButton(
            isRunning = uiState.isRunning,
            onStartPing = onStartPing,
            onStopPing = onStopPing
        )
        Spacer(modifier = Modifier.height(12.dp))
        PingSummaryCard(summary = uiState.summary)
        Spacer(modifier = Modifier.height(16.dp))
        PingResultsList(results = uiState.results)
    }
}

@Composable
private fun PingHostInput(
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
private fun PingActionButton(
    isRunning: Boolean,
    onStartPing: () -> Unit,
    onStopPing: () -> Unit
) {
    Button(
        onClick = if (isRunning) onStopPing else onStartPing,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = null
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        Text(if (isRunning) "Stop Ping" else "Start Ping")
    }
}

@Composable
private fun PingSummaryCard(summary: PingSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Ping Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sent/Recv: ${summary.transmitted}/${summary.received}", fontSize = 12.sp)
                Text(String.format("Loss: %.1f%%", summary.lossPercentage), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Min: ${formatRtt(summary.minRttMs)}", fontSize = 12.sp)
                Text("Avg: ${formatRtt(summary.avgRttMs)}", fontSize = 12.sp)
                Text("Max: ${formatRtt(summary.maxRttMs)}", fontSize = 12.sp)
                Text("StDev: ${formatRtt(summary.stdevRttMs)}", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PingResultsList(results: List<PingResult>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        reverseLayout = true
    ) {
        items(results.reversed(), key = { it.sequence }) { res ->
            PingResultRow(res = res)
        }
    }
}

@Composable
private fun PingResultRow(res: PingResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("#${res.sequence}", fontSize = 12.sp)
            Text(res.ip, fontSize = 12.sp)
            Text("${res.bytes} bytes", fontSize = 12.sp)
            Text("ttl=${res.ttl}", fontSize = 12.sp)
            Text(
                text = if (res.isSuccess && res.rttMs != null) String.format("%.1f ms", res.rttMs) else "Timeout",
                fontSize = 12.sp,
                color = if (res.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatRtt(rtt: Float?): String {
    return rtt?.let { String.format("%.1fms", it) } ?: "-"
}
