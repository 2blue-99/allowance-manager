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
import com.allowance.manager.core.domain.model.Allowance
import com.allowance.manager.core.domain.usecase.store.GetDailyAllowanceUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class BalanceWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun getDailyBalanceUseCase(): GetDailyAllowanceUseCase
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val getDailyAllowanceUseCase = entryPoint.getDailyBalanceUseCase()

        provideContent {
            val allowance by getDailyAllowanceUseCase().collectAsState(Allowance())
            GlanceTheme {
                BalanceWidgetContent(
                    allowance = allowance,
                    onRefresh = {
                        actionRunCallback<RefreshAction>()
                    },
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
