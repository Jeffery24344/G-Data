package com.gdata.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object Apps : Screen("apps", "Apps")
    data object DataSaver : Screen("data_saver", "Data Saver")
    data object Statistics : Screen("statistics", "Statistics")
    data object Network : Screen("network", "Network")
    data object Settings : Screen("settings", "Settings")
}
