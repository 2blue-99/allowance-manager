package com.allowance.manager.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.ui.theme.AmColors
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.allowance.manager.feature.account.navigation.AccountSettingRoute
import com.allowance.manager.feature.account.navigation.accountSettingScreen
import com.allowance.manager.feature.home.navigation.HomeRoute
import com.allowance.manager.feature.home.navigation.homeScreen
import com.allowance.manager.feature.intro.navigation.IntroRoute
import com.allowance.manager.feature.intro.navigation.introScreen
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
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(HomeRoute, HomeRoute::class, "홈", Icons.Filled.Home),
    BottomTab(StatsRoute, StatsRoute::class, "통계", Icons.Filled.BarChart),
)

@Composable
fun AppNavHost(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomTabs.any { currentDestination.isOn(it.kClass) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                // 커스텀 바텀바: NavigationBarItem의 고정된 인디케이터 여백을 피하려고 직접 구성.
                // Column이 흰 배경 + 시스템 내비바 여백을 직접 처리.
                Column(modifier = Modifier.background(AmColors.CardBg)) {
                    HorizontalDivider(thickness = 1.dp, color = AmColors.BarTrack)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        bottomTabs.forEach { tab ->
                            BottomBarItem(
                                selected = currentDestination.isOn(tab.kClass),
                                icon = tab.icon,
                                label = tab.label,
                                onClick = { navController.navigateToTab(tab.route) },
                            )
                        }
                    }
                    // 시스템 내비게이터(제스처 바) 영역만큼 흰 여백 확보
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(250)) },
            exitTransition = { fadeOut(tween(250)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(250)) },
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
                onNavigateToIntro = {
                    navController.navigate(IntroRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
            )
            introScreen(
                onFinish = {
                    navController.navigate(OnboardingRoute) {
                        popUpTo(IntroRoute) { inclusive = true }
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
                onNavigateToAccount = { navController.navigate(AccountSettingRoute) },
            )
            accountSettingScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * 커스텀 바텀바 아이템. 아이콘-라벨 간격을 직접 제어하고, 선택 시 포인트 컬러(초록)로 색상 애니메이션.
 */
@Composable
private fun RowScope.BottomBarItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (selected) AmColors.Emerald else AmColors.TextSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "bottomBarItemColor",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
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
