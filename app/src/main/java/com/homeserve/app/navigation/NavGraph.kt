package com.homeserve.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.homeserve.app.HomeServeApplication
import com.homeserve.app.ui.screens.auth.AuthScreen
import com.homeserve.app.ui.screens.auth.AuthViewModel
import com.homeserve.app.ui.screens.booking.BookingScreen
import com.homeserve.app.ui.screens.booking.BookingViewModel
import com.homeserve.app.ui.screens.home.HomeScreen
import com.homeserve.app.ui.screens.home.HomeViewModel
import com.homeserve.app.ui.screens.orderhistory.OrderHistoryScreen
import com.homeserve.app.ui.screens.orderhistory.OrderHistoryViewModel
import com.homeserve.app.ui.screens.paywall.PaywallScreen
import com.homeserve.app.ui.screens.paywall.PaywallViewModel
import com.homeserve.app.ui.screens.profile.ProfileScreen
import com.homeserve.app.ui.screens.profile.ProfileViewModel
import com.homeserve.app.ui.screens.provider.ProviderDetailScreen
import com.homeserve.app.ui.screens.provider.ProviderDetailViewModel
import com.homeserve.app.ui.screens.provider.ProviderListScreen
import com.homeserve.app.ui.screens.provider.ProviderListViewModel
import com.homeserve.app.ui.screens.splash.SplashScreen
import com.homeserve.app.ui.screens.splash.SplashViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    val context = LocalContext.current.applicationContext as HomeServeApplication
    val repository = context.repository
    val billingManager = context.billingManager

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            val vm: SplashViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SplashViewModel(repository) as T
                }
            })
            SplashScreen(viewModel = vm) { route ->
                navController.navigate(route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Login.route) {
            val vm: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(repository) as T
                }
            })
            AuthScreen(viewModel = vm) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Home.route) {
            val vm: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            })
            HomeScreen(
                viewModel = vm,
                onCategoryClick = { categoryId, city ->
                    navController.navigate(Screen.ProviderList.createRoute(categoryId, city))
                },
                onHistoryClick = {
                    navController.navigate(Screen.OrderHistory.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(
            route = Screen.ProviderList.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("city") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val city = backStackEntry.arguments?.getString("city") ?: ""
            val vm: ProviderListViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProviderListViewModel(repository, categoryId, city) as T
                }
            })
            ProviderListScreen(
                viewModel = vm,
                onBackClick = { navController.navigateUp() },
                onProviderClick = { providerId ->
                    navController.navigate(Screen.ProviderDetail.createRoute(providerId))
                }
            )
        }

        composable(
            route = Screen.ProviderDetail.route,
            arguments = listOf(
                navArgument("providerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
            val vm: ProviderDetailViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProviderDetailViewModel(repository, providerId) as T
                }
            })
            ProviderDetailScreen(
                viewModel = vm,
                onBackClick = { navController.navigateUp() },
                onBookClick = { pid ->
                    navController.navigate(Screen.Booking.createRoute(pid))
                }
            )
        }

        composable(
            route = Screen.Booking.route,
            arguments = listOf(
                navArgument("providerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
            val vm: BookingViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookingViewModel(repository, providerId) as T
                }
            })
            BookingScreen(
                viewModel = vm,
                onBackClick = { navController.navigateUp() },
                onBookingSuccess = {
                    navController.navigate(Screen.OrderHistory.route) {
                        popUpTo(Screen.Home.route) { saveState = false }
                    }
                },
                onGatedRedirect = {
                    navController.navigate(Screen.Paywall.route)
                }
            )
        }

        composable(Screen.Paywall.route) {
            val vm: PaywallViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PaywallViewModel(repository, billingManager) as T
                }
            })
            PaywallScreen(
                viewModel = vm,
                onCloseClick = { navController.navigateUp() },
                onPurchaseSuccess = {
                    navController.navigateUp()
                }
            )
        }

        composable(Screen.OrderHistory.route) {
            val vm: OrderHistoryViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OrderHistoryViewModel(repository) as T
                }
            })
            OrderHistoryScreen(
                viewModel = vm,
                onBackClick = { navController.navigateUp() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            val vm: ProfileViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(repository, billingManager) as T
                }
            })
            ProfileScreen(
                viewModel = vm,
                onBackClick = { navController.navigateUp() },
                onUpgradeClick = {
                    navController.navigate(Screen.Paywall.route)
                },
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
