package com.felixbrucker.tracer.feature.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.felixbrucker.tracer.core.database.entity.FavoriteTargetEntity

@Composable
fun TargetInputField(
    target: String,
    onTargetChange: (String) -> Unit,
    isRunning: Boolean,
    onStartTrace: () -> Unit,
    onStopTrace: () -> Unit,
    favorites: List<FavoriteTargetEntity>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedTextField(
            value = target,
            onValueChange = onTargetChange,
            label = { Text("Target Host / IP") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row {
                    if (favorites.isNotEmpty()) {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Favorite")
                        }
                    }
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
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                if (!isRunning && target.isNotBlank()) onStartTrace()
            })
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            favorites.forEach { favorite ->
                DropdownMenuItem(
                    text = {
                        Column {
                            if (!favorite.title.nullOrBlankSafe()) {
                                Text(
                                    text = favorite.title!!,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = favorite.host,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = favorite.host,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    onClick = {
                        onTargetChange(favorite.host)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun String?.nullOrBlankSafe(): Boolean {
    return this == null || this.trim().isEmpty()
}
