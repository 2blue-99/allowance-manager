package com.allowance.manager.feature.stats.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.stats.StatsRoute
import kotlinx.serialization.Serializable
import java.time.YearMonth

@Serializable
object StatsRoute

fun NavGraphBuilder.statsScreen(
    onOpenMonth: (YearMonth) -> Unit = {},
) {
    composable<StatsRoute> {
        StatsRoute(onOpenMonth = onOpenMonth)
    }
}
