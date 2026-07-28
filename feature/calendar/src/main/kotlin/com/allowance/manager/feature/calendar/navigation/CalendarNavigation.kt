package com.allowance.manager.feature.calendar.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.allowance.manager.feature.calendar.CalendarRoute
import kotlinx.serialization.Serializable

@Serializable
object CalendarRoute

fun NavGraphBuilder.calendarScreen() {
    composable<CalendarRoute> {
        CalendarRoute()
    }
}
