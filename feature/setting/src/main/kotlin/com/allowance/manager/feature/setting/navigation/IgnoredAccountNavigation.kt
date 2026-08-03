package com.allowance.manager.feature.setting.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.setting.IgnoredAccountRoute
import kotlinx.serialization.Serializable

@Serializable
object IgnoredAccountRoute

fun NavGraphBuilder.ignoredAccountScreen(
    onBack: () -> Unit,
) {
    composable<IgnoredAccountRoute> {
        IgnoredAccountRoute(onBack = onBack)
    }
}
