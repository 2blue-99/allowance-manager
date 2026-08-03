package com.allowance.manager.feature.setting.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.setting.SettingRoute
import kotlinx.serialization.Serializable

@Serializable
object SettingRoute

fun NavGraphBuilder.settingScreen(
    onBack: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToIgnored: () -> Unit,
    onNavigateToDebug: (() -> Unit)? = null,
) {
    composable<SettingRoute> {
        SettingRoute(
            onBack = onBack,
            onNavigateToAccount = onNavigateToAccount,
            onNavigateToIgnored = onNavigateToIgnored,
            onNavigateToDebug = onNavigateToDebug,
        )
    }
}
