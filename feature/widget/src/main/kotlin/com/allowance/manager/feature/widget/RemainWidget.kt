package com.allowance.manager.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.util.toCompactWon
import kotlin.math.roundToInt

private val SHOW_SPENT = booleanPreferencesKey("remain_show_spent")

/** ① 잔여 게이지 (2×2). 링 + 남은/지출 하단 토글(인스턴스별 상태). */
class RemainWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = context.widgetEntryPoint()
        val observeBudget = entryPoint.observeBudgetStatusUseCase()
        val observeType = entryPoint.getUserTypeUseCase()

        provideContent {
            val status by observeBudget().collectAsState(initial = null)
            val userType by observeType().collectAsState(UserType.Default)
            val showSpent = currentState<Preferences>()[SHOW_SPENT] ?: false
            GlanceTheme {
                RemainWidgetContent(
                    label = userType.label,
                    budget = status?.budget ?: 0L,
                    spent = status?.spent ?: 0L,
                    // 남은 = budget - spent + income. 홈과 동일하게 유즈케이스 계산값을 그대로 사용.
                    remaining = status?.remaining ?: 0L,
                    showSpent = showSpent,
                )
            }
        }
    }
}

@Composable
private fun RemainWidgetContent(label: String, budget: Long, spent: Long, remaining: Long, showSpent: Boolean) {
    val context = LocalContext.current
    val over = budget > 0 && remaining < 0
    val percentUsed = if (budget > 0) ((spent.toDouble() / budget) * 100).roundToInt() else 0
    val percentRemain = if (budget > 0) ((remaining.toDouble() / budget) * 100).roundToInt().coerceAtLeast(0) else 0
    val progress = when {
        budget <= 0 -> 0f
        over -> 1f
        showSpent -> (spent.toFloat() / budget).coerceIn(0f, 1f)
        else -> (remaining.toFloat() / budget).coerceIn(0f, 1f)
    }
    val amount = when {
        budget <= 0 || showSpent -> spent.toCompactWon()
        else -> remaining.toCompactWon()
    }
    // 모드별 문구: 지출=사용률, 남은=잔여율. 남은 모드에서 초과(음수)면 '예산 초과'.
    val sub = when {
        budget <= 0 -> "예산 미설정"
        showSpent -> "$percentUsed% 사용"
        over -> "예산 초과"
        else -> "$percentRemain% 남음"
    }

    val ring = ringBitmap(sizePx = 340, progress = progress, over = over)
    // 앱 열기는 링 영역에만 건다. (루트에 걸면 하단 토글과 중첩 clickable로 동시 발화 → 중복선택 버그)
    val openApp = actionStartActivity(context.launchAppIntent())

    // 링을 위젯 실제 크기에 맞춰 크게 (리사이즈 시 함께 커짐). 가장자리 여백을 둬 꽉 차지 않게.
    val size = LocalSize.current
    val ringMax = maxOf(96.dp, size.width - 30.dp)
    val ringSide = (size.height - 70.dp).coerceIn(96.dp, ringMax)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(AmColors.CardBg)
            .cornerRadius(20.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxWidth().clickable(openApp),
            contentAlignment = Alignment.Center,
        ) {
            Image(provider = ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.size(ringSide))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = amount,
                    style = TextStyle(
                        color = ColorProvider(AmColors.Navy),
                        fontSize = moneySp(amount, 26),
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = sub,
                    style = TextStyle(
                        color = ColorProvider(if (over) AmColors.Red else AmColors.TextSecondary),
                        fontSize = 11.sp,
                    ),
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        ModeToggle(showSpent = showSpent)
    }
}

@Composable
private fun ModeToggle(showSpent: Boolean) {
    // 회색 트랙 위에 흰색 pill(활성) — 하나만 흰 pill, 나머지는 투명(배타 선택).
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(AmColors.BarTrack)
            .cornerRadius(18.dp)
            .padding(3.dp),
    ) {
        ToggleSegment(
            text = "남음",
            active = !showSpent,
            onClick = actionRunCallback<SetRemainOnly>(),
            modifier = GlanceModifier.defaultWeight(),
        )
        ToggleSegment(
            text = "지출",
            active = showSpent,
            onClick = actionRunCallback<SetSpentOnly>(),
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}

@Composable
private fun ToggleSegment(text: String, active: Boolean, onClick: androidx.glance.action.Action, modifier: GlanceModifier) {
    Box(
        modifier = modifier
            // 두 세그먼트 모두 배경을 줘 반반 폭 전체가 탭되게 (비활성=트랙색으로 사실상 보이지 않음)
            .background(if (active) AmColors.CardBg else AmColors.BarTrack)
            .cornerRadius(15.dp)
            .padding(vertical = 7.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(if (active) AmColors.Navy else AmColors.TextSecondary),
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}

/** "남은" 탭 → 남은 모드로 고정(지출 해제). */
class SetRemainOnly : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { it[SHOW_SPENT] = false }
        RemainWidget().update(context, glanceId)
    }
}

/** "지출" 탭 → 지출 모드로 고정(남은 해제). */
class SetSpentOnly : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { it[SHOW_SPENT] = true }
        RemainWidget().update(context, glanceId)
    }
}

class RemainWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = RemainWidget()
}
