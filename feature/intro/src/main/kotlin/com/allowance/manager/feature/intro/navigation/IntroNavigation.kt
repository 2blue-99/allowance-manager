package com.allowance.manager.feature.intro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.intro.IntroRoute
import kotlinx.serialization.Serializable

@Serializable
object IntroRoute

fun NavGraphBuilder.introScreen(
    onFinish: () -> Unit,
) {
    composable<IntroRoute> {
        IntroRoute(onFinish = onFinish)
    }
}
