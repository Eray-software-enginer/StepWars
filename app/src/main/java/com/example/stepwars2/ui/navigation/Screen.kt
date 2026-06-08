package com.example.stepwars2.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Battle : Screen("battle")
    object Cards : Screen("cards")
    object Leaderboard : Screen("leaderboard")
    object Profile : Screen("profile")
    object Shop : Screen("shop")
    object Clan : Screen("clan")
    object ChestOpen : Screen("chest_open/{chestType}") {
        fun createRoute(chestType: String) = "chest_open/$chestType"
    }
}
