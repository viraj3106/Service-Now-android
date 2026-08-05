package com.homeserve.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object ProviderList : Screen("providerList/{categoryId}/{city}") {
        fun createRoute(categoryId: String, city: String) = "providerList/$categoryId/$city"
    }
    object ProviderDetail : Screen("providerDetail/{providerId}") {
        fun createRoute(providerId: String) = "providerDetail/$providerId"
    }
    object Booking : Screen("booking/{providerId}") {
        fun createRoute(providerId: String) = "booking/$providerId"
    }
    object Paywall : Screen("paywall")
    object OrderHistory : Screen("orderHistory")
    object Profile : Screen("profile")
}
