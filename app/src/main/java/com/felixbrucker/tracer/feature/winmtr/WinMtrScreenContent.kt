package com.felixbrucker.tracer.feature.winmtr

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.felixbrucker.tracer.core.model.MtrHopStats

@Composable
fun WinMtrScreenContent(
    statsList: List<MtrHopStats>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(statsList.size) {
        if (statsList.isNotEmpty()) {
            listState.animateScrollToItem(statsList.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 8.dp)
    ) {
        item {
            WinMtrHeaderRow()
        }
        items(statsList, key = { it.hopNumber }) { stat ->
            WinMtrHopCard(stat = stat)
        }
    }
}

@Composable
fun WinMtrHeaderRow() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", Modifier.width(28.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Host", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Loss%", Modifier.width(50.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("S/R", Modifier.width(55.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Avg", Modifier.width(45.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Best", Modifier.width(45.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Wrst", Modifier.width(45.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WinMtrHopCard(stat: MtrHopStats) {
    val cardColor by animateColorAsState(
        targetValue = when {
            stat.lossPercentage > 20.0 -> MaterialTheme.colorScheme.errorContainer
            stat.lossPercentage > 0.0 -> MaterialTheme.colorScheme.warningContainer()
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "cardColorAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stat.hopNumber}",
                modifier = Modifier.width(28.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stat.ipAddress,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${"%.0f".format(stat.lossPercentage)}%",
                modifier = Modifier.width(50.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (stat.lossPercentage > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${stat.sent}/${stat.received}",
                modifier = Modifier.width(55.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${"%.0f".format(stat.avgRttMs)}",
                modifier = Modifier.width(45.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (stat.bestRttMs == Double.MAX_VALUE) "-" else "${"%.0f".format(stat.bestRttMs)}",
                modifier = Modifier.width(45.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${"%.0f".format(stat.worstRttMs)}",
                modifier = Modifier.width(45.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun androidx.compose.material3.ColorScheme.warningContainer() =
    this.tertiaryContainer
