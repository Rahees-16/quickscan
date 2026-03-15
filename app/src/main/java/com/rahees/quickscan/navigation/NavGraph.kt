package com.rahees.quickscan.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rahees.quickscan.ui.generator.GeneratorScreen
import com.rahees.quickscan.ui.history.HistoryScreen
import com.rahees.quickscan.ui.result.ScanResultScreen
import com.rahees.quickscan.ui.scanner.ScannerScreen
import com.rahees.quickscan.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object ScannerRoute

@Serializable
data class ScanResultRoute(
    val content: String,
    val format: String,
    val type: String
)

@Serializable
object HistoryRoute

@Serializable
object GeneratorRoute

@Serializable
object SettingsRoute

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem("Scanner", Icons.Default.QrCodeScanner, ScannerRoute),
        BottomNavItem("History", Icons.Default.History, HistoryRoute),
        BottomNavItem("Generator", Icons.Default.QrCode, GeneratorRoute),
        BottomNavItem("Settings", Icons.Default.Settings, SettingsRoute)
    )

    val showBottomBar = currentDestination?.let { dest ->
        bottomNavItems.any { item ->
            when (item.route) {
                is ScannerRoute -> dest.hasRoute<ScannerRoute>()
                is HistoryRoute -> dest.hasRoute<HistoryRoute>()
                is GeneratorRoute -> dest.hasRoute<GeneratorRoute>()
                is SettingsRoute -> dest.hasRoute<SettingsRoute>()
                else -> false
            }
        }
    } ?: true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.let { dest ->
                            when (item.route) {
                                is ScannerRoute -> dest.hasRoute<ScannerRoute>()
                                is HistoryRoute -> dest.hasRoute<HistoryRoute>()
                                is GeneratorRoute -> dest.hasRoute<GeneratorRoute>()
                                is SettingsRoute -> dest.hasRoute<SettingsRoute>()
                                else -> false
                            }
                        } ?: false

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ScannerRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<ScannerRoute> {
                ScannerScreen(
                    onScanResult = { content, format, type ->
                        navController.navigate(
                            ScanResultRoute(
                                content = content,
                                format = format,
                                type = type
                            )
                        )
                    }
                )
            }

            composable<ScanResultRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ScanResultRoute>()
                ScanResultScreen(
                    content = route.content,
                    format = route.format,
                    type = route.type,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<HistoryRoute> {
                HistoryScreen(
                    onItemClick = { content, format, type ->
                        navController.navigate(
                            ScanResultRoute(
                                content = content,
                                format = format,
                                type = type
                            )
                        )
                    }
                )
            }

            composable<GeneratorRoute> {
                GeneratorScreen()
            }

            composable<SettingsRoute> {
                SettingsScreen()
            }
        }
    }
}
