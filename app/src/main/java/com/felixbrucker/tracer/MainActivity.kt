package com.felixbrucker.tracer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracerAppScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onStartTrace: (String, TraceType) -> Unit,
    onStopTrace: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var targetHost by remember { mutableStateOf("8.8.8.8") }

    val pingResults by viewModel.pingResults.collectAsState()
    val tracerouteHops by viewModel.tracerouteHops.collectAsState()
    val winMtrStats by viewModel.winMtrStats.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val activeTraceType by viewModel.activeTraceType.collectAsState()

    val isRunning = activeTraceType != null

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
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = "WinMTR") },
                    label = { Text("WinMTR") }
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
                            0 -> TraceType.WINMTR
                            1 -> TraceType.TRACEROUTE
                            else -> TraceType.PING
                        }
                        onStartTrace(targetHost, currentType)
                    },
                    onStopTrace = onStopTrace
                )
            }

            when (selectedTab) {
                0 -> WinMtrScreenContent(statsList = winMtrStats)
                1 -> TracerouteScreenContent(hops = tracerouteHops)
                2 -> PingScreenContent(results = pingResults)
                3 -> FavoritesScreenContent(
                    favorites = favorites,
                    onSelectFavorite = { favorite ->
                        targetHost = favorite.host
                        val type = runCatching { TraceType.valueOf(favorite.defaultType) }.getOrDefault(TraceType.WINMTR)
                        selectedTab = when (type) {
                            TraceType.WINMTR -> 0
                            TraceType.TRACEROUTE -> 1
                            TraceType.PING -> 2
                        }
                        onStartTrace(favorite.host, type)
                    },
                    onAddFavorite = { title, host, type ->
                        viewModel.addFavorite(title, host, type)
                    },
                    onRemoveFavorite = { id ->
                        viewModel.removeFavorite(id)
                    }
                )
            }
        }
    }
}
