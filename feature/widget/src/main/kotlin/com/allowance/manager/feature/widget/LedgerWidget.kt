package com.allowance.manager.feature.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.domain.util.toCompactWon

/** ④ 가계부 (2×1 → 4×1). 이번 달(달력 월) 지출·수입 2분할. */
class LedgerWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val observeLedger = context.widgetEntryPoint().observeMonthlyLedgerUseCase()
        provideContent {
            val ledger by observeLedger().collectAsState(initial = null)
            GlanceTheme {
                CellsWidgetContent(
                    listOf(
                        CellData("이번 달 지출", (ledger?.expense ?: 0L).toCompactWon(), AmColors.Red),
                        CellData("이번 달 수입", (ledger?.income ?: 0L).toCompactWon(), AmColors.EmeraldDark),
                    ),
                )
            }
        }
    }
}

class LedgerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = LedgerWidget()
}
