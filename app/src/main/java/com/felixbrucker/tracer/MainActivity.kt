package com.felixbrucker.tracer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.felixbrucker.tracer.core.designsystem.TracerTheme
import com.felixbrucker.tracer.core.model.TraceType
import com.felixbrucker.tracer.core.network.TraceForegroundService
import com.felixbrucker.tracer.feature.components.TargetInputField
import com.felixbrucker.tracer.feature.favorites.FavoritesScreenContent
import com.felixbrucker.tracer.feature.main.MainViewModel
import com.felixbrucker.tracer.feature.ping.PingScreenContent
import com.felixbrucker.tracer.feature.traceroute.TracerouteScreenContent
import com.felixbrucker.tracer.feature.winmtr.WinMtrScreenContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            TracerTheme {
                TracerAppScreen(
                    onStartTrace = { target, type ->
                        TraceForegroundService.start(this, target, type)
                    },
                    onStopTrace = {
                        TraceForegroundService.stop(this)
                    }
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracerAppScreen(
    viewModel: MainViewModel = viewModel(),
    onStartTrace: (String, TraceType) -> Unit,
    onStopTrace: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var targetHost by remember { mutableStateOf("") }

    val pingResults by viewModel.pingResults.collectAsState()
    val tracerouteHops by viewModel.tracerouteHops.collectAsState()
    val winMtrStats by viewModel.winMtrStats.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val activeTraceType by viewModel.activeTraceType.collectAsState()

    val isRunning = activeTraceType != null

    LaunchedEffect(favorites) {
        if (targetHost.isEmpty() && favorites.isNotEmpty()) {
            targetHost = favorites.first().host
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tracer - Network Diagnostics") }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = "MTR") },
                    label = { Text("MTR") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Route, contentDescription = "Traceroute") },
                    label = { Text("Traceroute") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.NetworkCheck, contentDescription = "Ping") },
                    label = { Text("Ping") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "Favorites") },
                    label = { Text("Favorites") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab != 3) {
                TargetInputField(
                    target = targetHost,
                    onTargetChange = { targetHost = it },
                    isRunning = isRunning,
                    onStartTrace = {
                        val currentType = when (selectedTab) {
                            0 -> TraceType.MTR
                            1 -> TraceType.TRACEROUTE
                            else -> TraceType.PING
                        }
                        onStartTrace(targetHost, currentType)
                    },
                    onStopTrace = onStopTrace,
                    favorites = favorites
                )
            }

            when (selectedTab) {
                0 -> WinMtrScreenContent(statsList = winMtrStats)
                1 -> TracerouteScreenContent(hops = tracerouteHops)
                2 -> PingScreenContent(
                    results = pingResults,
                    isRunning = isRunning,
                    targetHost = targetHost
                )
                3 -> FavoritesScreenContent(
                    favorites = favorites,
                    onStartTraceWithType = { host, type ->
                        targetHost = host
                        selectedTab = when (type) {
                            TraceType.MTR -> 0
                            TraceType.TRACEROUTE -> 1
                            TraceType.PING -> 2
                        }
                        onStartTrace(host, type)
                    },
                    onAddFavorite = { title, host ->
                        viewModel.addFavorite(title, host)
                    },
                    onUpdateFavorite = { id, title, host ->
                        viewModel.updateFavorite(id, title, host)
                    },
                    onRemoveFavorite = { id ->
                        viewModel.removeFavorite(id)
                    }
                )
            }
        }
    }
}
