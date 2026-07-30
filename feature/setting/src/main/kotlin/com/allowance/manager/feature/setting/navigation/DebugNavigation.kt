package com.allowance.manager.feature.setting.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.setting.DebugRoute
import kotlinx.serialization.Serializable

@Serializable
object DebugRoute

fun NavGraphBuilder.debugScreen(
    onBack: () -> Unit,
    onNavigateToIntro: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
) {
    composable<DebugRoute> {
        DebugRoute(
            onBack = onBack,
            onNavigateToIntro = onNavigateToIntro,
            onNavigateToOnboarding = onNavigateToOnboarding,
        )
    }
}
