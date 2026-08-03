package com.allowance.manager.feature.home.navigation

import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Rect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

fun NavGraphBuilder.homeScreen(guideTargets: SnapshotStateMap<String, Rect>) {
    composable<HomeRoute> {
        HomeRoute(guideTargets = guideTargets)
    }
}
