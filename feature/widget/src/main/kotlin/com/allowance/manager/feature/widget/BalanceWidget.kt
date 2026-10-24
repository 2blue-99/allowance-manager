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
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

/** ② 심플 한 줄 (2×1 → 4×1). 남은 금액 + 게이지 + 양끝(지출/예산). */
class BalanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 위젯 세션은 홈 화면에 있는 동안 계속 살아 updateAll 로는 provideGlance 가 다시 돌지 않는다.
        // '오늘'을 호출 시점에 고정하면 월급일이 지나도 옛 사이클을 보여주므로, 자정마다 스스로 재구독한다.
        val observeBudget = context.widgetEntryPoint().observeBudgetStatusUseCase()
        val budgetFlow = todayTicks().flatMapLatest { observeBudget(it) }
        provideContent {
            val status by budgetFlow.collectAsState(initial = null)
            GlanceTheme {
                SimpleBalanceContent(
                    budget = status?.totalBudget ?: 0L,   // 총 예산(기본+추가) — 홈과 동일 기준
                    spent = status?.spent ?: 0L,
                    // 남은 = budget - spent + income. 홈과 동일하게 유즈케이스 계산값을 그대로 사용.
                    remaining = status?.remaining ?: 0L,
                )
            }
        }
    }
}
