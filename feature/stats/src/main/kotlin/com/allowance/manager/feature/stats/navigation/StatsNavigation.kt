package com.allowance.manager.feature.stats.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.stats.StatsRoute
import kotlinx.serialization.Serializable

@Serializable
object StatsRoute

fun NavGraphBuilder.statsScreen() {
    composable<StatsRoute> {
        StatsRoute()
    }
}
