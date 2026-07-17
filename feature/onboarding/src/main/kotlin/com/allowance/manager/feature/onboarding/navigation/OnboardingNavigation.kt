package com.allowance.manager.feature.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.onboarding.OnboardingRoute
import kotlinx.serialization.Serializable

@Serializable
object OnboardingRoute

fun NavGraphBuilder.onboardingScreen(
    onFinish: () -> Unit,
) {
    composable<OnboardingRoute> {
        OnboardingRoute(onFinish = onFinish)
    }
}
