package com.felixbrucker.tracer.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.felixbrucker.tracer.core.datastore.TracerSettings

@Composable
fun SettingsScreenContent(
    settings: TracerSettings,
    onUpdatePingIntervalMs: (Long) -> Unit,
    onUpdatePingTimeoutSec: (Int) -> Unit,
    onUpdateMaxHops: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Diagnostic Preferences",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = settings.pingIntervalMs.toString(),
                    onValueChange = { input ->
                        input.toLongOrNull()?.let { if (it >= 200) onUpdatePingIntervalMs(it) }
                    },
                    label = { Text("Ping & MTR Interval (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = settings.pingTimeoutSec.toString(),
                    onValueChange = { input ->
                        input.toIntOrNull()?.let { if (it in 1..30) onUpdatePingTimeoutSec(it) }
                    },
                    label = { Text("Probe Timeout Duration (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = settings.maxHops.toString(),
                    onValueChange = { input ->
                        input.toIntOrNull()?.let { if (it in 1..64) onUpdateMaxHops(it) }
                    },
                    label = { Text("Maximum Hop Limit (Traceroute & MTR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
