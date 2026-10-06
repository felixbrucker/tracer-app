package com.felixbrucker.tracer.feature.traceroute

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
import com.felixbrucker.tracer.core.model.TracerouteHop

@Composable
fun TracerouteScreen(
    uiState: TracerouteUiState,
    onHostChanged: (String) -> Unit,
    onStartTraceroute: () -> Unit,
    onStopTraceroute: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TracerouteHostInput(
            host = uiState.targetHost,
            isFavorite = uiState.isFavorite,
            onHostChanged = onHostChanged,
            onToggleFavorite = onToggleFavorite
        )
        Spacer(modifier = Modifier.height(12.dp))
        TracerouteActionButton(
            isRunning = uiState.isRunning,
            onStartTraceroute = onStartTraceroute,
            onStopTraceroute = onStopTraceroute
        )
        Spacer(modifier = Modifier.height(16.dp))
        TracerouteHopList(hops = uiState.hops)
    }
}

@Composable
private fun TracerouteHostInput(
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
private fun TracerouteActionButton(
    isRunning: Boolean,
    onStartTraceroute: () -> Unit,
    onStopTraceroute: () -> Unit
) {
    Button(
        onClick = if (isRunning) onStopTraceroute else onStartTraceroute,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = null
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        Text(if (isRunning) "Stop Traceroute" else "Start Traceroute")
    }
}

@Composable
private fun TracerouteHopList(hops: List<TracerouteHop>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(hops, key = { it.hopNumber }) { hop ->
            TracerouteHopCard(hop = hop)
        }
    }
}

@Composable
private fun TracerouteHopCard(hop: TracerouteHop) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${hop.hopNumber}.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hop.host,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (hop.ip != null && hop.ip != hop.host) {
                    Text(
                        text = hop.ip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = formatRttTimes(hop),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatRttTimes(hop: TracerouteHop): String {
    val r1 = hop.rtt1Ms?.let { String.format("%.0fms", it) } ?: "*"
    val r2 = hop.rtt2Ms?.let { String.format("%.0fms", it) } ?: "*"
    val r3 = hop.rtt3Ms?.let { String.format("%.0fms", it) } ?: "*"
    return "$r1  $r2  $r3"
}
