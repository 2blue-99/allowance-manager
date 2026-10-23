package com.allowance.manager.feature.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.allowance.manager.core.domain.util.todayTicks
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.domain.util.toCompactWon

/** ④ 가계부 (2×1 → 4×1). 이번 달(수급일 사이클) 지출·수입 2분할. */
class LedgerWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 위젯 세션은 홈 화면에 있는 동안 계속 살아 updateAll 로는 provideGlance 가 다시 돌지 않는다.
        // '오늘'을 호출 시점에 고정하면 월급일이 지나도 옛 사이클을 보여주므로, 자정마다 스스로 재구독한다.
        val observeLedger = context.widgetEntryPoint().observeMonthlyLedgerUseCase()
        val ledgerFlow = todayTicks().flatMapLatest { observeLedger(it) }
        provideContent {
            val ledger by ledgerFlow.collectAsState(initial = null)
            GlanceTheme {
                CellsWidgetContent(
                    listOf(
                        CellData("이번 달 지출", (ledger?.expense ?: 0L).toCompactWon(), AmColors.Red),
                        CellData("이번 달 수입", (ledger?.income ?: 0L).toCompactWon(), AmColors.EmeraldDark),
                    ),
                    valueBase = 20,
                )
            }
        }
    }
}

class LedgerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = LedgerWidget()
}
