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

/** ③ 오늘 코치 (4×1). 오늘 사용 · 오늘 권장 · 하루 평균 3분할. */
class DailyCoachWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 위젯 세션은 홈 화면에 있는 동안 계속 살아 updateAll 로는 provideGlance 가 다시 돌지 않는다.
        // '오늘'을 호출 시점에 고정하면 월급일이 지나도 옛 사이클을 보여주므로, 자정마다 스스로 재구독한다.
        val observeCoach = context.widgetEntryPoint().observeDailyCoachUseCase()
        val coachFlow = todayTicks().flatMapLatest { observeCoach(it) }
        provideContent {
            val coach by coachFlow.collectAsState(initial = null)
            val today = coach?.todaySpent ?: 0L
            val recommend = coach?.recommendedPerDay ?: 0L
            val avg = coach?.avgPerDay ?: 0L

            // 오늘 사용이 권장 이하면 초록, 넘으면 빨강 (권장 0=초과 상태)
            val todayColor = when {
                recommend > 0 && today <= recommend -> AmColors.EmeraldDark
                today == 0L -> AmColors.Navy
                else -> AmColors.Red
            }

            GlanceTheme {
                CellsWidgetContent(
                    listOf(
                        CellData("오늘 사용", today.toCompactWon(), todayColor),
                        CellData("오늘 권장", recommend.toCompactWon(), AmColors.Navy),
                        CellData("하루 평균", avg.toCompactWon(), AmColors.Navy),
                    ),
                )
            }
        }
    }
}

class DailyCoachWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DailyCoachWidget()
}
