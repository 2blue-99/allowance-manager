package com.allowance.manager.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
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
import kotlin.reflect.KClass

private data class BottomTab(
    val route: Any,
    val kClass: KClass<*>,
    val label: String,
    val icon: String,
)

private val bottomTabs = listOf(
    BottomTab(HomeRoute, HomeRoute::class, "홈", "🏠"),
    BottomTab(StatsRoute, StatsRoute::class, "통계", "📊"),
)

@Composable
fun AppNavHost(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomTabs.any { currentDestination.isOn(it.kClass) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDestination.isOn(tab.kClass),
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Text(tab.icon) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier.padding(innerPadding),
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
                onNavigateToSetting = { navController.navigate(SettingRoute) },
            )
            statsScreen()
            settingScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun NavDestination?.isOn(route: KClass<*>): Boolean =
    this?.hierarchy?.any { it.hasRoute(route) } == true

private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        // 홈(탭 루트)까지 팝 + 상태 저장 → 탭 전환 시 각 화면 상태 보존
        popUpTo(HomeRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
