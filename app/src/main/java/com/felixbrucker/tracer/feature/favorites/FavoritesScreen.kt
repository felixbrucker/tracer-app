package com.felixbrucker.tracer.feature.favorites

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.felixbrucker.tracer.core.model.FavoriteTarget
import com.felixbrucker.tracer.core.model.TraceMode

@Composable
fun FavoritesScreen(
    favorites: List<FavoriteTarget>,
    onAddFavorite: (String, String, TraceMode) -> Unit,
    onRemoveFavorite: (FavoriteTarget) -> Unit,
    onLaunchTarget: (FavoriteTarget) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Favorite")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Favorite Targets", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            FavoritesList(
                favorites = favorites,
                onRemoveFavorite = onRemoveFavorite,
                onLaunchTarget = onLaunchTarget
            )
        }

        if (showDialog) {
            AddFavoriteDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name, host, mode ->
                    onAddFavorite(name, host, mode)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
private fun FavoritesList(
    favorites: List<FavoriteTarget>,
    onRemoveFavorite: (FavoriteTarget) -> Unit,
    onLaunchTarget: (FavoriteTarget) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(favorites, key = { it.id }) { favorite ->
            FavoriteCard(
                favorite = favorite,
                onRemoveFavorite = onRemoveFavorite,
                onLaunchTarget = onLaunchTarget
            )
        }
    }
}

@Composable
private fun FavoriteCard(
    favorite: FavoriteTarget,
    onRemoveFavorite: (FavoriteTarget) -> Unit,
    onLaunchTarget: (FavoriteTarget) -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = favorite.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${favorite.host} (${favorite.defaultMode.name})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onLaunchTarget(favorite) }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Launch", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { onRemoveFavorite(favorite) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddFavoriteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, TraceMode) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TraceMode.LIVE_MTR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Favorite Target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host or IP") },
                    singleLine = true
                )
                Text("Default Tool:", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == TraceMode.LIVE_MTR,
                        onClick = { mode = TraceMode.LIVE_MTR }
                    )
                    Text("Live MTR", modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == TraceMode.REGULAR_TRACEROUTE,
                        onClick = { mode = TraceMode.REGULAR_TRACEROUTE }
                    )
                    Text("Traceroute", modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == TraceMode.PING,
                        onClick = { mode = TraceMode.PING }
                    )
                    Text("Ping", modifier = Modifier.padding(start = 4.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, host, mode) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
