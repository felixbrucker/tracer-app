package com.felixbrucker.tracer.feature.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun TargetInputField(
    target: String,
    onTargetChange: (String) -> Unit,
    isRunning: Boolean,
    onStartTrace: () -> Unit,
    onStopTrace: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = target,
        onValueChange = onTargetChange,
        label = { Text("Target Host / IP") },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        trailingIcon = {
            if (isRunning) {
                IconButton(onClick = onStopTrace) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                }
            } else {
                IconButton(
                    onClick = onStartTrace,
                    enabled = target.isNotBlank()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = {
            if (!isRunning && target.isNotBlank()) onStartTrace()
        })
    )
}
