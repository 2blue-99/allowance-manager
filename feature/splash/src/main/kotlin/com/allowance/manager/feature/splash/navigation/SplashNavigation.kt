package com.allowance.manager.feature.splash.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.splash.SplashScreen
import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

fun NavGraphBuilder.splashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
) {
    composable<SplashRoute> {
        SplashScreen(
            onNavigateToHome = onNavigateToHome,
            onNavigateToOnboarding = onNavigateToOnboarding,
        )
    }
}
