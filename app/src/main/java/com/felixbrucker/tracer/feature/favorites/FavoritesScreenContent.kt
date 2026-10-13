package com.felixbrucker.tracer.feature.favorites

import androidx.compose.foundation.clickable
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
import com.felixbrucker.tracer.core.database.entity.FavoriteTargetEntity
import com.felixbrucker.tracer.core.model.TraceType

@Composable
fun FavoritesScreenContent(
    favorites: List<FavoriteTargetEntity>,
    onStartTraceWithType: (String, TraceType) -> Unit,
    onAddFavorite: (String?, String) -> Unit,
    onRemoveFavorite: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var favoriteForTypeSelection by remember { mutableStateOf<FavoriteTargetEntity?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Favorite")
            }
        }
    ) { paddingValues ->
        if (favorites.isEmpty()) {
            Text(
                text = "No saved favorites yet. Tap '+' to add one.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                items(favorites, key = { it.id }) { favorite ->
                    FavoriteItemCard(
                        favorite = favorite,
                        onSelect = { favoriteForTypeSelection = favorite },
                        onDelete = { onRemoveFavorite(favorite.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddFavoriteDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, host ->
                    onAddFavorite(title, host)
                    showAddDialog = false
                }
            )
        }

        favoriteForTypeSelection?.let { favorite ->
            TraceTypeSelectionDialog(
                target = favorite,
                onDismiss = { favoriteForTypeSelection = null },
                onTypeSelected = { traceType ->
                    onStartTraceWithType(favorite.host, traceType)
                    favoriteForTypeSelection = null
                }
            )
        }
    }
}

@Composable
fun FavoriteItemCard(
    favorite: FavoriteTargetEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!favorite.title.isNullOrBlank()) {
                    Text(
                        text = favorite.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = favorite.host,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = favorite.host,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = onSelect) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start Trace")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Favorite")
            }
        }
    }
}

@Composable
fun AddFavoriteDialog(
    onDismiss: () -> Unit,
    onAdd: (String?, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Favorite Target") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Name / Title (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host or IP (e.g., 8.8.8.8)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (host.isNotBlank()) onAdd(title.ifBlank { null }, host) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TraceTypeSelectionDialog(
    target: FavoriteTargetEntity,
    onDismiss: () -> Unit,
    onTypeSelected: (TraceType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Diagnostic Action") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Target: ${target.title ?: target.host}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onTypeSelected(TraceType.MTR) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("MTR Live Trace")
                }
                Button(
                    onClick = { onTypeSelected(TraceType.TRACEROUTE) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("Traceroute")
                }
                Button(
                    onClick = { onTypeSelected(TraceType.PING) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("Ping")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
