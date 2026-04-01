package com.allowance.manager.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

fun NavGraphBuilder.homeScreen(
    onNavigateToSetting: () -> Unit,
) {
    composable<HomeRoute> {
        HomeRoute(onNavigateToSetting = onNavigateToSetting)
    }
}
