package com.allowance.manager.feature.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetStatusUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.map

class BalanceWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun observeBudgetStatusUseCase(): ObserveBudgetStatusUseCase
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val observeBudgetStatusUseCase = entryPoint.observeBudgetStatusUseCase()

        provideContent {
            val pair by observeBudgetStatusUseCase()
                .map { it.spent to it.budget }
                .collectAsState(0L to 0L)
            GlanceTheme {
                BalanceWidgetContent(
                    spent = pair.first,
                    budget = pair.second,
                    onRefresh = { actionRunCallback<RefreshAction>() },
                )
            }
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        BalanceWidget().update(context, glanceId)
    }
}
