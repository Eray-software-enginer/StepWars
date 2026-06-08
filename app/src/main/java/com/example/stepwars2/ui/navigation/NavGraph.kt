package com.example.stepwars2.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.stepwars2.ui.components.BottomNavBar
import com.example.stepwars2.ui.viewmodel.BattleState
import com.example.stepwars2.ui.viewmodel.BattleViewModel
import com.example.stepwars2.data.repository.UserStateManager
import com.example.stepwars2.ui.screens.auth.LoginScreen
import com.example.stepwars2.ui.screens.auth.RegisterScreen
import com.example.stepwars2.ui.screens.battle.BattleScreen
import com.example.stepwars2.ui.screens.cards.CardsScreen
import com.example.stepwars2.ui.screens.chest.ChestOpenScreen
import com.example.stepwars2.ui.screens.home.HomeScreen
import com.example.stepwars2.ui.screens.leaderboard.LeaderboardScreen
import com.example.stepwars2.ui.screens.profile.ProfileScreen
import com.example.stepwars2.ui.screens.shop.ShopScreen
import com.example.stepwars2.ui.screens.clan.ClanScreen
import com.example.stepwars2.ui.screens.splash.SplashScreen
import com.example.stepwars2.ui.viewmodel.AuthViewModel

// Screens that show the bottom navigation bar
private val bottomNavScreens = listOf(
    Screen.Home.route,
    Screen.Battle.route,
    Screen.Cards.route,
    Screen.Leaderboard.route,
    Screen.Profile.route,
    Screen.Shop.route,
    Screen.Clan.route
)

@Composable
fun StepWarsNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavScreens

    // Shared AuthViewModel at NavGraph level
    val authViewModel: AuthViewModel = viewModel()
    // Battle state tracking — savaş sırasında alt navigasyonu gizle
    val battleViewModel: BattleViewModel = viewModel()
    val battleState by battleViewModel.battleState.collectAsStateWithLifecycle()
    val isBattleActive = battleState is BattleState.Searching || battleState is BattleState.InBattle

    Scaffold(
        containerColor = Color(0xFF0D1117),
        bottomBar = {
            if (showBottomBar && !isBattleActive) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to the Home destination to avoid building up a large
                            // stack of destinations on the back stack
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when re-selecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    viewModel = authViewModel
                )
            }

            // Login Screen
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    viewModel = authViewModel
                )
            }

            // Register Screen
            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    viewModel = authViewModel
                )
            }

            // Home Screen (main tab)
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToBattle = {
                        navController.navigate(Screen.Battle.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToShop = {
                        navController.navigate(Screen.Shop.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Battle Screen — shared ViewModel
            composable(Screen.Battle.route) {
                BattleScreen(viewModel = battleViewModel)
            }

            // Cards Screen
            composable(Screen.Cards.route) {
                CardsScreen()
            }

            // Leaderboard Screen
            composable(Screen.Leaderboard.route) {
                LeaderboardScreen()
            }

            // Profile Screen
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onSignOut = {
                        UserStateManager.stopListening()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Shop Screen
            composable(Screen.Shop.route) {
                ShopScreen(
                    onOpenChest = { chestType ->
                        navController.navigate(Screen.ChestOpen.createRoute(chestType))
                    }
                )
            }

            // Clan Screen
            composable(Screen.Clan.route) {
                ClanScreen()
            }

            // Chest Open Screen
            composable(Screen.ChestOpen.route) { backStackEntry ->
                val chestType = backStackEntry.arguments?.getString("chestType") ?: "BRONZE"
                ChestOpenScreen(
                    chestTypeStr = chestType,
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}
