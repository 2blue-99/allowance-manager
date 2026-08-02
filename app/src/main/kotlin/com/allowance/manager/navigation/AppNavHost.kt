package com.allowance.manager.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.anim.AmMotion
import com.allowance.manager.core.designsystem.component.amRippleClickable
import com.allowance.manager.core.designsystem.theme.AmColors
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.allowance.manager.feature.account.navigation.AccountSettingRoute
import com.allowance.manager.feature.account.navigation.accountSettingScreen
import com.allowance.manager.feature.calendar.navigation.CalendarRoute
import com.allowance.manager.feature.calendar.navigation.calendarScreen
import com.allowance.manager.feature.home.navigation.HomeRoute
import com.allowance.manager.feature.home.navigation.homeScreen
import com.allowance.manager.feature.intro.navigation.IntroRoute
import com.allowance.manager.feature.intro.navigation.introScreen
import com.allowance.manager.feature.onboarding.navigation.OnboardingRoute
import com.allowance.manager.feature.onboarding.navigation.onboardingScreen
import com.allowance.manager.StartDestination
import com.allowance.manager.BuildConfig
import com.allowance.manager.feature.setting.navigation.DebugRoute
import com.allowance.manager.feature.setting.navigation.debugScreen
import com.allowance.manager.feature.setting.navigation.SettingRoute
import com.allowance.manager.feature.setting.navigation.settingScreen
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
    BottomTab(CalendarRoute(), CalendarRoute::class, "월별", Icons.Filled.CalendarMonth),
    BottomTab(StatsRoute, StatsRoute::class, "통계", Icons.Filled.BarChart),
)

@Composable
fun AppNavHost(navController: NavHostController, startDestination: StartDestination) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // 더보기(설정)도 탭처럼 취급 → 진입해도 바텀바 유지
    val showBottomBar = bottomTabs.any { currentDestination.isOn(it.kClass) } ||
        currentDestination.isOn(SettingRoute::class)
    val startRoute: Any = when (startDestination) {
        StartDestination.INTRO -> IntroRoute
        StartDestination.ONBOARDING -> OnboardingRoute
        StartDestination.HOME -> HomeRoute
    }

    // 홈에서 뒤로가기: 한 번은 안내, 2초 내 두 번이면 종료
    val context = LocalContext.current
    var lastBackAt by remember { mutableStateOf(0L) }
    BackHandler(enabled = currentDestination.isOn(HomeRoute::class)) {
        val now = System.currentTimeMillis()
        if (now - lastBackAt < 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackAt = now
            Toast.makeText(context, "한 번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        bottomBar = {
            // 팝인 대신 슬라이드로 부드럽게 등장/퇴장
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
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
                        // 더보기(설정)도 다른 탭과 동일한 전환 방식 → 탭 간 이동 일관성 유지
                        BottomBarItem(
                            selected = currentDestination.isOn(SettingRoute::class),
                            icon = Icons.Filled.MoreHoriz,
                            label = "더보기",
                            onClick = { navController.navigateToTab(SettingRoute) },
                        )
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
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding),
            // 온보딩 완료 → 홈은 가로 슬라이드(공유축)로 "앞으로 진행" 느낌, 그 외는 페이드
            enterTransition = {
                if (initialState.destination.isOn(OnboardingRoute::class) && targetState.destination.isOn(HomeRoute::class)) {
                    AmMotion.slideForwardEnter(durationMs = 350)
                } else {
                    AmMotion.fadeEnter()
                }
            },
            exitTransition = {
                if (initialState.destination.isOn(OnboardingRoute::class) && targetState.destination.isOn(HomeRoute::class)) {
                    AmMotion.slideForwardExit(durationMs = 350)
                } else {
                    AmMotion.fadeExit()
                }
            },
            popEnterTransition = { AmMotion.fadeEnter() },
            popExitTransition = { AmMotion.fadeExit() },
        ) {
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
            homeScreen()
            calendarScreen()
            statsScreen(
                onOpenMonth = { ym ->
                    navController.navigate(CalendarRoute(month = ym.toString())) {
                        popUpTo(HomeRoute) { saveState = true }
                        launchSingleTop = true
                    }
                },
            )
            settingScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAccount = { navController.navigate(AccountSettingRoute) },
                // debug 빌드에서만 디버그 진입 노출
                onNavigateToDebug = if (BuildConfig.DEBUG) {
                    { navController.navigate(DebugRoute) }
                } else {
                    null
                },
            )
            accountSettingScreen(
                onBack = { navController.popBackStack() },
            )
            debugScreen(
                onBack = { navController.popBackStack() },
                onNavigateToIntro = { navController.navigate(IntroRoute) },
                onNavigateToOnboarding = { navController.navigate(OnboardingRoute) },
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
            .amRippleClickable(onClick = onClick),
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
