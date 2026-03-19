package com.allowance.manager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.allowance.manager.feature.home.navigation.HomeRoute
import com.allowance.manager.feature.home.navigation.homeScreen
import com.allowance.manager.feature.setting.navigation.settingScreen
import com.allowance.manager.feature.splash.navigation.SplashRoute
import com.allowance.manager.feature.splash.navigation.splashScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = SplashRoute
    ) {
        splashScreen(
            onNavigateToHome = {
                navController.navigate(HomeRoute) {
                    popUpTo(SplashRoute) { inclusive = true }
                }
            }
        )
        homeScreen(
            onNavigateToSetting = {
                navController.navigate(com.allowance.manager.feature.setting.navigation.SettingRoute)
            }
        )
        settingScreen(
            onBack = { navController.popBackStack() }
        )
    }
}
