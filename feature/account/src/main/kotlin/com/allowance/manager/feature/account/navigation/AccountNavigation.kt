package com.allowance.manager.feature.account.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.core.designsystem.anim.AmMotion
import com.allowance.manager.feature.account.AccountSettingRoute
import kotlinx.serialization.Serializable

@Serializable
object AccountSettingRoute

fun NavGraphBuilder.accountSettingScreen(
    onBack: () -> Unit,
) {
    // 더보기 → 계좌 관리: 좌우 슬라이드로 전체화면이 밀려 들어오고/나가도록 (바텀바 페이드아웃 가림)
    composable<AccountSettingRoute>(
        enterTransition = { AmMotion.slideForwardEnter() },
        exitTransition = { AmMotion.slideForwardExit() },
        popEnterTransition = { AmMotion.slideBackwardEnter() },
        popExitTransition = { AmMotion.slideBackwardExit() },
    ) {
        AccountSettingRoute(onBack = onBack)
    }
}
