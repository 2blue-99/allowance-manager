package com.allowance.manager.feature.setting.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.setting.SettingScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingRoute

fun NavGraphBuilder.settingScreen(
    onBack: () -> Unit,
) {
    composable<SettingRoute> {
        SettingScreen(onBack = onBack)
    }
}
