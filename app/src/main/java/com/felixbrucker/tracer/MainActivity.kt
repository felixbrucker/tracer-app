package com.felixbrucker.tracer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.felixbrucker.tracer.core.designsystem.TracerTheme
import com.felixbrucker.tracer.core.model.TraceMode
import com.felixbrucker.tracer.feature.favorites.FavoritesScreen
import com.felixbrucker.tracer.feature.favorites.FavoritesViewModel
import com.felixbrucker.tracer.feature.mtr.LiveMtrScreen
import com.felixbrucker.tracer.feature.mtr.LiveMtrViewModel
import com.felixbrucker.tracer.feature.ping.PingScreen
import com.felixbrucker.tracer.feature.ping.PingViewModel
import com.felixbrucker.tracer.feature.settings.SettingsScreen
import com.felixbrucker.tracer.feature.settings.SettingsViewModel
import com.felixbrucker.tracer.feature.traceroute.TracerouteScreen
import com.felixbrucker.tracer.feature.traceroute.TracerouteViewModel
import dagger.hilt.android.AndroidEntryPoint

sealed class NavigationItem(val route: String, val title: String, val icon: ImageVector) {
    data object LiveMtr : NavigationItem("live_mtr", "Live MTR", Icons.AutoMirrored.Filled.List)
    data object Traceroute : NavigationItem("traceroute", "Traceroute", Icons.Filled.Timeline)
    data object Ping : NavigationItem("ping", "Ping", Icons.Filled.NetworkCheck)
    data object Favorites : NavigationItem("favorites", "Favorites", Icons.Filled.Favorite)
    data object Settings : NavigationItem("settings", "Settings", Icons.Filled.Settings)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TracerTheme {
                TracerApp()
            }
        }
    }
}

@Composable
fun TracerApp() {
    val navController = rememberNavController()
    val navItems = listOf(
        NavigationItem.LiveMtr,
        NavigationItem.Traceroute,
        NavigationItem.Ping,
        NavigationItem.Favorites,
        NavigationItem.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            TracerBottomBar(navController = navController, items = navItems)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationItem.LiveMtr.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavigationItem.LiveMtr.route) {
                val vm: LiveMtrViewModel = hiltViewModel()
                val uiState by vm.uiState.collectAsState()
                LiveMtrScreen(
                    uiState = uiState,
                    onHostChanged = vm::onHostChanged,
                    onStartTrace = vm::startTrace,
                    onPauseTrace = vm::pauseTrace,
                    onResumeTrace = vm::resumeTrace,
                    onStopTrace = vm::stopTrace,
                    onResetStats = vm::resetStats,
                    onToggleFavorite = vm::toggleFavorite
                )
            }
            composable(NavigationItem.Traceroute.route) {
                val vm: TracerouteViewModel = hiltViewModel()
                val uiState by vm.uiState.collectAsState()
                TracerouteScreen(
                    uiState = uiState,
                    onHostChanged = vm::onHostChanged,
                    onStartTraceroute = vm::startTraceroute,
                    onStopTraceroute = vm::stopTraceroute,
                    onToggleFavorite = vm::toggleFavorite
                )
            }
            composable(NavigationItem.Ping.route) {
                val vm: PingViewModel = hiltViewModel()
                val uiState by vm.uiState.collectAsState()
                PingScreen(
                    uiState = uiState,
                    onHostChanged = vm::onHostChanged,
                    onStartPing = vm::startPing,
                    onStopPing = vm::stopPing,
                    onToggleFavorite = vm::toggleFavorite
                )
            }
            composable(NavigationItem.Favorites.route) {
                val vm: FavoritesViewModel = hiltViewModel()
                val favorites by vm.favorites.collectAsState()
                val mtrVm: LiveMtrViewModel = hiltViewModel()
                val tracerouteVm: TracerouteViewModel = hiltViewModel()
                val pingVm: PingViewModel = hiltViewModel()

                FavoritesScreen(
                    favorites = favorites,
                    onAddFavorite = vm::addFavorite,
                    onRemoveFavorite = vm::removeFavorite,
                    onLaunchTarget = { target ->
                        when (target.defaultMode) {
                            TraceMode.LIVE_MTR -> {
                                mtrVm.onHostChanged(target.host)
                                navController.navigate(NavigationItem.LiveMtr.route)
                            }
                            TraceMode.REGULAR_TRACEROUTE -> {
                                tracerouteVm.onHostChanged(target.host)
                                navController.navigate(NavigationItem.Traceroute.route)
                            }
                            TraceMode.PING -> {
                                pingVm.onHostChanged(target.host)
                                navController.navigate(NavigationItem.Ping.route)
                            }
                        }
                    }
                )
            }
            composable(NavigationItem.Settings.route) {
                val vm: SettingsViewModel = hiltViewModel()
                val settings by vm.settings.collectAsState()
                SettingsScreen(
                    settings = settings,
                    onSaveSettings = vm::updateSettings
                )
            }
        }
    }
}

@Composable
private fun TracerBottomBar(
    navController: NavHostController,
    items: List<NavigationItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
