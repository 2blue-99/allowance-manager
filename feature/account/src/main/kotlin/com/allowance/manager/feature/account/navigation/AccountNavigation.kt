package com.allowance.manager.feature.account.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.account.AccountSettingRoute
import kotlinx.serialization.Serializable

@Serializable
object AccountSettingRoute

fun NavGraphBuilder.accountSettingScreen(
    onBack: () -> Unit,
) {
    composable<AccountSettingRoute> {
        AccountSettingRoute(onBack = onBack)
    }
}
