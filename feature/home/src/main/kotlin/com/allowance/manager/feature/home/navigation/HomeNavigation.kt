package com.allowance.manager.feature.home.navigation

import androidx.annotation.DrawableRes
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Rect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

fun NavGraphBuilder.homeScreen(
    guideTargets: SnapshotStateMap<String, Rect>,
    // 홈 가이드 위젯 스텝 배선 — feature 간 의존을 피해 app(위젯 모듈 접근)에서 주입.
    widgetPinSupported: Boolean = false,
    @DrawableRes widgetPreviewRes: Int = 0,
    onAddWidget: () -> Unit = {},
) {
    composable<HomeRoute> {
        HomeRoute(
            guideTargets = guideTargets,
            widgetPinSupported = widgetPinSupported,
            widgetPreviewRes = widgetPreviewRes,
            onAddWidget = onAddWidget,
        )
    }
}
