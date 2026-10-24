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
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.util.toCompactWon

/** ③ 오늘 코치 (4×1). 금일 지출 · 하루 지출 권장 · 월급일까지 3분할 — 홈 예산 카드와 같은 값. */
class DailyCoachWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 위젯 세션은 홈 화면에 있는 동안 계속 살아 updateAll 로는 provideGlance 가 다시 돌지 않는다.
        // '오늘'을 호출 시점에 고정하면 월급일이 지나도 옛 사이클을 보여주므로, 자정마다 스스로 재구독한다.
        val entryPoint = context.widgetEntryPoint()
        val observeCoach = entryPoint.observeDailyCoachUseCase()
        val observeType = entryPoint.getUserTypeUseCase()
        val coachFlow = todayTicks().flatMapLatest { observeCoach(it) }
        provideContent {
            val coach by coachFlow.collectAsState(initial = null)
            val userType by observeType().collectAsState(UserType.Default)
            val today = coach?.todaySpent ?: 0L
            val recommend = coach?.recommendedPerDay ?: 0L
            val dday = coach?.daysUntilPayday ?: 0

            // 금일 지출이 권장 이하면 초록, 넘으면 빨강 (홈 예산 카드와 같은 기준)
            val todayColor = when {
                today == 0L -> AmColors.Navy
                coach?.isOverToday == true -> AmColors.Red
                else -> AmColors.EmeraldDark
            }

            GlanceTheme {
                CellsWidgetContent(
                    listOf(
                        CellData("금일 지출", today.toCompactWon(), todayColor),
                        CellData("하루 지출 권장", recommend.toCompactWon(), AmColors.Navy),
                        CellData("${userType.paydayShort}까지", if (dday <= 0) "오늘" else "D-$dday", AmColors.EmeraldDark),
                    ),
                )
            }
        }
    }
}

class DailyCoachWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DailyCoachWidget()
}
