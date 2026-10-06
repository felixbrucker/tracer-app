package com.felixbrucker.tracer.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.felixbrucker.tracer.core.model.TraceSettings

@Composable
fun SettingsScreen(
    settings: TraceSettings,
    onSaveSettings: (TraceSettings) -> Unit
) {
    var maxHops by remember(settings) { mutableStateOf(settings.maxHops.toString()) }
    var packetSize by remember(settings) { mutableStateOf(settings.packetSize.toString()) }
    var timeoutMs by remember(settings) { mutableStateOf(settings.timeoutMs.toString()) }
    var intervalMs by remember(settings) { mutableStateOf(settings.intervalMs.toString()) }
    var resolveDns by remember(settings) { mutableStateOf(settings.resolveDns) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Network Diagnostics Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = maxHops,
            onValueChange = { maxHops = it },
            label = { Text("Max Hops (1-64)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = packetSize,
            onValueChange = { packetSize = it },
            label = { Text("Packet Size (bytes)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = timeoutMs,
            onValueChange = { timeoutMs = it },
            label = { Text("Timeout (ms)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = intervalMs,
            onValueChange = { intervalMs = it },
            label = { Text("MTR Interval (ms)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Resolve Hostnames (DNS)", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = resolveDns,
                onCheckedChange = { resolveDns = it }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val updated = TraceSettings(
                    maxHops = maxHops.toIntOrNull() ?: settings.maxHops,
                    packetSize = packetSize.toIntOrNull() ?: settings.packetSize,
                    timeoutMs = timeoutMs.toIntOrNull() ?: settings.timeoutMs,
                    intervalMs = intervalMs.toLongOrNull() ?: settings.intervalMs,
                    resolveDns = resolveDns
                )
                onSaveSettings(updated)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }
    }
}
