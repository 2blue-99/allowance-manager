package com.allowance.manager.feature.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmLineTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.lastDay
import com.allowance.manager.core.domain.usecase.budget.PaydayChangePreview
import java.time.LocalDate

/** 말일 — payday 0은 매월 마지막 날로 clamp된다 */
private const val PAYDAY_EOM = 0

/**
 * 변경 결과 미리보기 계산. (경계, 규칙일) → 저장했을 때의 모습. 계산 중이거나 실패하면 null.
 * 저장 로직과 같은 함수를 쓰는 UseCase가 뒤에 있어 예고 = 결과.
 */
typealias PaydayPreview = suspend (boundary: LocalDate, payday: Int) -> PaydayChangePreview?

/**
 * ① 월급일(규칙일) 다이얼로그 — "앞으로 매달 며칠에 받나요?" 질문 하나.
 *
 * 이번 회차의 **시작은 건드리지 않고 끝만** 새 규칙으로 다시 계산된다. 받은 날 정정은 [CycleStartDialog]가 맡는다.
 *
 * @param onSave 새 규칙일 (1~31, 0=말일)
 */
@Composable
fun PaydayRuleDialog(
    currentCycle: BudgetCycle,
    currentPayday: Int,
    title: String,
    preview: PaydayPreview,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // 말일은 대표값 31로 보여주되 저장은 EOM으로
    var input by remember { mutableStateOf(if (currentPayday in 1..31) currentPayday.toString() else "31") }
    var isEom by remember { mutableStateOf(currentPayday == PAYDAY_EOM) }
    val day = input.toIntOrNull()?.takeIf { it in 1..31 }
    val payday = if (isEom) PAYDAY_EOM else day

    var result by remember { mutableStateOf<PaydayChangePreview?>(null) }
    LaunchedEffect(payday) {
        result = payday?.let { preview(currentCycle.start, it) }
    }

    AmDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { payday?.let(onSave) },
        confirmEnabled = payday != null && payday != currentPayday,
        analyticsTag = AmAnalytics.Dialog.PAYDAY,
    ) {
        Text("앞으로 매달 며칠에 받나요?", style = AmType.size12_medium, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.sm))
        AmLineTextField(
            value = input,
            onValueChange = { v ->
                val digits = v.filter { it.isDigit() }.take(2)
                input = digits.toIntOrNull()?.coerceIn(1, 31)?.toString() ?: ""
                isEom = false
            },
            hint = "예) 25",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AmSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(AmSpacing.sm)) {
            AmChip("15일", !isEom && day == 15) { input = "15"; isEom = false }
            AmChip("20일", !isEom && day == 20) { input = "20"; isEom = false }
            AmChip("25일", !isEom && day == 25) { input = "25"; isEom = false }
            AmChip("말일", isEom) { input = "31"; isEom = true }
        }

        Spacer(Modifier.height(AmSpacing.lg))
        // 카드는 항상 같은 행 구성으로 그린다 — 값이 없을 땐 "—". 입력마다 높이가 널뛰지 않게.
        val p = result
        PreviewCard(
            rows = listOf(
                // 시작은 그대로, 끝만 바뀐 것을 강조
                "이번 회차" to (p?.let { periodText(it.thisCycle, highlightStart = false, highlightEnd = it.thisCycle.endExclusive != currentCycle.endExclusive) } ?: DASH),
                "다음 회차" to (p?.let {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = AmColors.Emerald)) { append(it.nextStart.korean()) }
                        append("부터")
                    }
                } ?: DASH),
            ),
            note = "앞으로 매월 ${payday?.paydayText() ?: "—"}에 받는 걸로 계산해요. 주말·공휴일이면 앞의 평일로 당겨요.",
        )
    }
}

/**
 * ② 이번 회차 다이얼로그 — "이번에 실제로 받은 날은?" 질문 하나.
 *
 * 일(日) 숫자만 받고 **가장 최근에 지나간 그 날**로 푼다(오늘 9/12에 22 → 8/22). 그래서 미래는 고를 수 없고
 * 경계 이동도 최대 한 달 전으로 자연히 제한된다. 규칙일은 그대로 — 회차의 **시작만** 옮긴다.
 *
 * "이번 달만 다른 날 받은" 일회성도 이걸로 받은 당일 정정하면 된다(규칙을 안 건드리니 자동으로 이번 달만 예외).
 *
 * @param onSave 해석된 실제 받은 날
 */
@Composable
fun CycleStartDialog(
    currentCycle: BudgetCycle,
    currentPayday: Int,
    preview: PaydayPreview,
    onSave: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    // 초기값은 현재 회차의 **실제** 시작일(주말·공휴일 보정된 날). 규칙이 25일이어도 24일에 받았으면 "24".
    var input by remember { mutableStateOf(currentCycle.start.dayOfMonth.toString()) }
    // 손대기 전엔 현재 시작일 그대로 — 회차가 한 달을 넘긴 경우 같은 일(日)이 이달에 다시 와도 재해석하지 않는다
    var touched by remember { mutableStateOf(false) }
    val day = input.toIntOrNull()?.takeIf { it in 1..31 }
    val resolved = if (!touched) currentCycle.start else day?.let { BudgetCycle.recentDate(it, today) }

    var result by remember { mutableStateOf<PaydayChangePreview?>(null) }
    LaunchedEffect(resolved) {
        result = resolved?.let { preview(it, currentPayday) }
    }

    AmDialog(
        title = "이번 회차",
        onDismiss = onDismiss,
        onConfirm = { resolved?.let(onSave) },
        confirmEnabled = resolved != null && resolved != currentCycle.start,
        analyticsTag = AmAnalytics.Dialog.PAYDAY_OVERRIDE,
    ) {
        Text("이번에 실제로 받은 날은 며칠인가요?", style = AmType.size12_medium, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.sm))
        AmLineTextField(
            value = input,
            onValueChange = { v -> input = v.filter { it.isDigit() }.take(2); touched = true },
            hint = "예) 25",
            keyboardType = KeyboardType.Number,
            // 해석 결과를 항상 명시 — "22"가 어느 달 22일인지 오해하지 않게
            supportingText = when {
                !touched -> "= ${currentCycle.start.korean()} (지금 회차 시작일)"
                resolved != null -> "= ${resolved.korean()}로 기록돼요"
                day != null -> "최근 두 달 안에 없는 날짜예요"
                else -> "1~31 사이 숫자를 넣어주세요"
            },
            supportingTextColor = if (touched && resolved != null) AmColors.Emerald else AmColors.TextTertiary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(AmSpacing.lg))
        // 카드·행·설명문을 항상 고정 — 값이 없을 땐 "—". 지난 회차가 없는 건 온보딩 첫 회차 앞을 찍은 경우뿐이다.
        val p = result
        PreviewCard(
            rows = listOf(
                "이번 회차" to (p?.let { periodText(it.thisCycle, highlightStart = true, highlightEnd = false) } ?: DASH),
                "지난 회차" to (p?.previousCycle?.let { periodText(it, highlightStart = false, highlightEnd = true) } ?: DASH),
            ),
            note = "매달 받는 날(${currentPayday.paydayText()})은 그대로예요. 지난 회차는 받은 날 전날까지로 정리돼요.",
        )
    }
}

// ─────────────────────────── 공용 ───────────────────────────

/** 값이 아직 없을 때의 자리표시 — 카드 높이를 유지한다 */
private val DASH = androidx.compose.ui.text.AnnotatedString("—")

/** 변경 결과 예고 카드 — 라벨/값 행 + 하단 설명. 행 수·설명 줄 수를 고정해 입력마다 높이가 변하지 않게 한다. */
@Composable
private fun PreviewCard(
    rows: List<Pair<String, androidx.compose.ui.text.AnnotatedString>>,
    note: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AmShape.cardSmall)
            .background(AmColors.ScreenBg)
            .padding(AmSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AmSpacing.xs),
    ) {
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = AmType.size12_medium, color = AmColors.TextSecondary)
                Text(value, style = AmType.size12_bold, color = AmColors.TextPrimary)
            }
        }
        HorizontalDivider(color = AmColors.BarTrack)
        Text(note, style = AmType.size10_medium, color = AmColors.TextSecondary)
    }
}

/** "8월 25일 ~ 9월 24일" — 바뀐 쪽만 강조색 */
private fun periodText(cycle: BudgetCycle, highlightStart: Boolean, highlightEnd: Boolean) = buildAnnotatedString {
    if (highlightStart) withStyle(SpanStyle(color = AmColors.Emerald)) { append(cycle.start.korean()) } else append(cycle.start.korean())
    append(" ~ ")
    if (highlightEnd) withStyle(SpanStyle(color = AmColors.Emerald)) { append(cycle.lastDay.korean()) } else append(cycle.lastDay.korean())
}

/** "8월 25일" */
private fun LocalDate.korean(): String = "${monthValue}월 ${dayOfMonth}일"

/** "25일" / "말일" */
private fun Int.paydayText(): String = if (this <= 0) "말일" else "${this}일"
