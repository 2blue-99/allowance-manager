package com.allowance.manager.feature.splash.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
``import com.allowance.manager.feature.splash.SplashRoute
import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

fun NavGraphBuilder.splashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToIntro: () -> Unit,
) {
    composable<SplashRoute> {
        SplashRoute(
            onNavigateToHome = onNavigateToHome,
            onNavigateToOnboarding = onNavigateToOnboarding,
            onNavigateToIntro = onNavigateToIntro,
        )
    }
}
