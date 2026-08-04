package com.allowance.manager.feature.setting.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.core.designsystem.anim.AmMotion
import com.allowance.manager.feature.setting.IgnoredAccountRoute
import kotlinx.serialization.Serializable

@Serializable
object IgnoredAccountRoute

fun NavGraphBuilder.ignoredAccountScreen(
    onBack: () -> Unit,
) {
    // 더보기 → 무시 계좌 관리: 계좌 관리와 동일한 좌우 슬라이드 전체화면 전환
    composable<IgnoredAccountRoute>(
        enterTransition = { AmMotion.slideForwardEnter() },
        exitTransition = { AmMotion.slideForwardExit() },
        popEnterTransition = { AmMotion.slideBackwardEnter() },
        popExitTransition = { AmMotion.slideBackwardExit() },
    ) {
        IgnoredAccountRoute(onBack = onBack)
    }
}
