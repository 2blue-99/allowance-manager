package com.allowance.manager.feature.calendar.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.allowance.manager.feature.calendar.CalendarRoute
import kotlinx.serialization.Serializable
import java.time.YearMonth

/** month = "yyyy-MM" 딥링크(통계 → 해당 달). null = 기본(이번 달) */
@Serializable
data class CalendarRoute(val month: String? = null)

fun NavGraphBuilder.calendarScreen() {
    composable<CalendarRoute> { entry ->
        val initialMonth = entry.toRoute<CalendarRoute>().month
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        CalendarRoute(initialMonth = initialMonth)
    }
}
