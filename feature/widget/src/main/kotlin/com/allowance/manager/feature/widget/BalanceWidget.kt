package com.allowance.manager.feature.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

/** ② 심플 한 줄 (2×1 → 4×1). 남은 금액 + 게이지 + 양끝(지출/예산). */
class BalanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val observeBudget = context.widgetEntryPoint().observeBudgetStatusUseCase()
        provideContent {
            val status by observeBudget().collectAsState(initial = null)
            GlanceTheme {
                SimpleBalanceContent(
                    budget = status?.budget ?: 0L,
                    spent = status?.spent ?: 0L,
                )
            }
        }
    }
}
