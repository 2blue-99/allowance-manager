package com.allowance.manager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.allowance.manager.feature.home.navigation.HomeRoute
import com.allowance.manager.feature.home.navigation.homeScreen
import com.allowance.manager.feature.onboarding.navigation.OnboardingRoute
import com.allowance.manager.feature.onboarding.navigation.onboardingScreen
import com.allowance.manager.feature.setting.navigation.SettingRoute
import com.allowance.manager.feature.setting.navigation.settingScreen
import com.allowance.manager.feature.splash.navigation.SplashRoute
import com.allowance.manager.feature.splash.navigation.splashScreen
import com.allowance.manager.feature.stats.navigation.StatsRoute
import com.allowance.manager.feature.stats.navigation.statsScreen

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
            },
            onNavigateToOnboarding = {
                navController.navigate(OnboardingRoute) {
                    popUpTo(SplashRoute) { inclusive = true }
                }
            },
        )
        onboardingScreen(
            onFinish = {
                navController.navigate(HomeRoute) {
                    popUpTo(OnboardingRoute) { inclusive = true }
                }
            },
        )
        homeScreen(
            onNavigateToSetting = {
                navController.navigate(SettingRoute)
            },
            onNavigateToStats = {
                navController.navigate(StatsRoute)
            },
        )
        statsScreen(
            onBack = { navController.popBackStack() }
        )
        settingScreen(
            onBack = { navController.popBackStack() }
        )
    }
}
