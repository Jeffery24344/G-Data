package com.gdata.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gdata.app.ui.apps.AppsScreen
import com.gdata.app.ui.datasaver.DataSaverScreen
import com.gdata.app.ui.home.HomeScreen
import com.gdata.app.ui.network.NetworkScreen
import com.gdata.app.ui.privacy.PrivacyScreen
import com.gdata.app.ui.settings.SettingsScreen
import com.gdata.app.ui.statistics.StatisticsScreen
import com.gdata.app.ui.theme.Primary

data class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Apps, Icons.Filled.Apps, Icons.Outlined.Apps),
    BottomNavItem(Screen.DataSaver, Icons.Filled.Speed, Icons.Outlined.Speed),
    BottomNavItem(Screen.Statistics, Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem(Screen.Network, Icons.Filled.NetworkCheck, Icons.Outlined.NetworkCheck)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val hideChrome = currentDestination?.route == Screen.Settings.route ||
        currentDestination?.route == Screen.Privacy.route

    Scaffold(
        topBar = {
            if (!hideChrome) {
                TopAppBar(
                    title = {
                        val title = bottomNavItems
                            .find { it.screen.route == currentDestination?.route }
                            ?.screen?.title ?: "G Data"
                        Text(title, fontWeight = FontWeight.Bold)
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (!hideChrome) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.screen.title
                                )
                            },
                            label = {
                                Text(item.screen.title, style = MaterialTheme.typography.labelSmall)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                indicatorColor = Primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Apps.route) { AppsScreen() }
            composable(Screen.DataSaver.route) { DataSaverScreen() }
            composable(Screen.Statistics.route) { StatisticsScreen() }
            composable(Screen.Network.route) { NetworkScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onPrivacy = { navController.navigate(Screen.Privacy.route) }
                )
            }
            composable(Screen.Privacy.route) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
