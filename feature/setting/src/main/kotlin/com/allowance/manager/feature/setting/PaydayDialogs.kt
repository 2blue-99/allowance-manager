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
import com.allowance.manager.core.designsystem.component.AmSegmented
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

/** 시작 이동(받았던 날) 예고 — 끝은 현재 회차 것을 유지한다. 규칙 변경 예고([PaydayPreview])와 계산이 다르다. */
typealias CycleStartPreview = suspend (boundary: LocalDate) -> PaydayChangePreview?

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
    today: LocalDate = LocalDate.now(),
) {
    // 말일은 대표값 31로 보여주되 저장은 EOM으로
    var input by remember { mutableStateOf(if (currentPayday in 1..31) currentPayday.toString() else "31") }
    var isEom by remember { mutableStateOf(currentPayday == PAYDAY_EOM) }
    val day = input.toIntOrNull()?.takeIf { it in 1..31 }
    val payday = if (isEom) PAYDAY_EOM else day

    var result by remember { mutableStateOf<PaydayChangePreview?>(null) }
    // 다음 회차도 기간으로 보여준다 — 새 끝에서 새 규칙으로 시작하는 사이클 = (다음 시작, 새 규칙)
    var nextCycle by remember { mutableStateOf<BudgetCycle?>(null) }
    LaunchedEffect(payday) {
        val p = payday?.let { preview(currentCycle.start, it) }
        result = p
        nextCycle = p?.let { preview(it.nextStart, payday)?.thisCycle }
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
        val endChanged = p != null && p.thisCycle.endExclusive != currentCycle.endExclusive
        val next = nextCycle?.takeIf { it.start == p?.nextStart }   // 입력이 바뀐 직후 옛 계산이 잠깐 남는 것 방지
        PreviewCard(
            rows = listOf(
                // 시작은 그대로, 끝만 바뀐 것을 강조
                "이번 회차" to (p?.let { periodText(it.thisCycle, highlightStart = false, highlightEnd = endChanged) } ?: DASH),
                "다음 회차" to (next?.let { periodText(it, highlightStart = endChanged, highlightEnd = endChanged) } ?: DASH),
            ),
            // 새 규칙의 다음 지급일이 이미 지났으면(말일→10일을 12일에 바꾸는 경우) 저장 즉시 회차가 넘어간다 — 미리 알린다
            note = if (p != null && !p.nextStart.isAfter(today)) {
                "${p.nextStart.korean()}이 이미 지나서 저장하면 바로 다음 회차가 이번 회차가 돼요. 앞으로 매월 ${payday?.paydayText() ?: "—"}에 받는 걸로 계산해요."
            } else {
                "앞으로 매월 ${payday?.paydayText() ?: "—"}에 받는 걸로 계산해요. 주말·공휴일이면 앞의 평일로 당겨요."
            },
        )
    }
}

/** 이번 회차 다이얼로그의 두 모드 — 어느 경계를 고치는지. 선언 순서 = 탭 순서(받을 날이 먼저·기본) */
private enum class CycleEdge(val label: String) {
    /** 이번 회차 끝 = 다음 받을 날 (> 오늘). "이번 회차만" 고정, 규칙일은 그대로. 대부분의 용례라 기본 탭 */
    END("받을 날"),
    /** 이번 회차 시작 = 이번에 받았던 날 (≤ 오늘). 지난 회차 끝이 따라 움직인다 */
    START("받았던 날"),
}

/**
 * ② 이번 회차 다이얼로그 — [받았던 날 | 받을 날] 두 모드. 규칙일은 두 모드 모두 건드리지 않는다.
 *
 * - 받았던 날: 일(日) 숫자를 **가장 최근에 지나간 그 날**로 풀어 회차 **시작**을 옮긴다(오늘 9/12에 22 → 8/22).
 *   미래는 고를 수 없고 경계 이동도 최대 한 달 전으로 자연히 제한된다.
 * - 받을 날: 일 숫자를 **가장 가까운 다가올 그 날**로 풀어 회차 **끝**을 그 날로 고정한다(20 → 9/20).
 *   "이번 달만 다른 날 받는" 일회성 — 그 뒤 회차는 다시 규칙일로 계산된다.
 *
 * 기본 탭은 '받을 날' — "이번 달은 며칠에 받는다"가 가장 흔한 용례고, 새 사용자는 고칠 시작이 없다.
 *
 * 질문은 "회차가 끝나는 날"이 아니라 **"월급일이 언제냐"** 로 묻는다 — 사용자가 넣는 숫자는 돈이 들어오는 날이고,
 * 코드에서도 그 날이 회차 경계(endExclusive/start)다. 회차 표시(~ 전날)는 그대로.
 *
 * @param paydayLabel 유형별 호칭 — "월급일"/"용돈일"
 * @param preview 규칙 기준 예고 — 받을 날 모드의 '다음 회차'(고른 날에서 규칙으로 시작하는 회차) 계산용
 * @param previewStart 시작 이동 예고 — 끝은 현재 회차 것을 유지 (받았던 날 모드)
 * @param onSaveStart 해석된 실제 받은 날 (시작 정정)
 * @param onSaveEnd 해석된 다음 받을 날 (끝 고정)
 */
@Composable
fun CycleAdjustDialog(
    currentCycle: BudgetCycle,
    currentPayday: Int,
    paydayLabel: String,
    preview: PaydayPreview,
    previewStart: CycleStartPreview,
    onSaveStart: (LocalDate) -> Unit,
    onSaveEnd: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    var edge by remember { mutableStateOf(CycleEdge.END) }

    // 모드별 입력은 따로 보관 — 탭을 오가도 각자 값이 남는다.
    // 초기값은 현재 회차의 **실제** 경계(주말·공휴일 보정된 날). 규칙이 25일이어도 24일에 받았으면 "24".
    var startInput by remember { mutableStateOf(currentCycle.start.dayOfMonth.toString()) }
    var endInput by remember { mutableStateOf(currentCycle.endExclusive.dayOfMonth.toString()) }
    // 손대기 전엔 현재 값 그대로 — 회차가 한 달을 넘긴 경우 같은 일(日)이 이달에 다시 와도 재해석하지 않는다
    var startTouched by remember { mutableStateOf(false) }
    var endTouched by remember { mutableStateOf(false) }

    val startDay = startInput.toIntOrNull()?.takeIf { it in 1..31 }
    val endDay = endInput.toIntOrNull()?.takeIf { it in 1..31 }
    val resolvedStart = if (!startTouched) currentCycle.start else startDay?.let { BudgetCycle.recentDate(it, today) }
    // 끝은 시작보다 뒤여야 한다 (오늘보다 뒤면 자동으로 만족하지만 방어)
    val resolvedEnd = if (!endTouched) currentCycle.endExclusive
    else endDay?.let { BudgetCycle.upcomingDate(it, today) }?.takeIf { it.isAfter(currentCycle.start) }

    // 둘 다 저장 로직과 같은 계산을 쓰는 UseCase로 예고한다.
    // - 시작 정정: 시작만 새 날로, 끝은 현재 회차 것 유지 → 이번·지난 회차
    // - 끝 고정: 이번 회차 끝은 고른 날 자체고, **다음 회차**는 그 날에서 규칙으로 시작하는 사이클 = (새 끝, 현재 규칙)
    var startResult by remember { mutableStateOf<PaydayChangePreview?>(null) }
    LaunchedEffect(resolvedStart) {
        startResult = resolvedStart?.let { previewStart(it) }
    }
    var nextAfterEnd by remember { mutableStateOf<BudgetCycle?>(null) }
    LaunchedEffect(resolvedEnd) {
        nextAfterEnd = resolvedEnd?.let { preview(it, currentPayday)?.thisCycle }
    }

    val confirmEnabled = when (edge) {
        CycleEdge.START -> startTouched && resolvedStart != null && resolvedStart != currentCycle.start
        CycleEdge.END -> endTouched && resolvedEnd != null && resolvedEnd != currentCycle.endExclusive
    }

    AmDialog(
        title = "이번 회차",
        onDismiss = onDismiss,
        onConfirm = {
            when (edge) {
                CycleEdge.START -> resolvedStart?.let(onSaveStart)
                CycleEdge.END -> resolvedEnd?.let(onSaveEnd)
            }
        },
        confirmEnabled = confirmEnabled,
        analyticsTag = AmAnalytics.Dialog.PAYDAY_OVERRIDE,
        analyticsParams = mapOf(AmAnalytics.Param.TYPE to edge.name.lowercase()),
    ) {
        AmSegmented(
            options = CycleEdge.entries.map { it.label },
            selectedIndex = edge.ordinal,
            onSelect = { edge = CycleEdge.entries[it] },
        )
        Spacer(Modifier.height(AmSpacing.md))

        when (edge) {
            CycleEdge.START -> {
                Text("이번 ${paydayLabel}은 언제였나요?", style = AmType.size12_medium, color = AmColors.TextSecondary)
                Spacer(Modifier.height(AmSpacing.sm))
                AmLineTextField(
                    value = startInput,
                    onValueChange = { v -> startInput = v.filter { it.isDigit() }.take(2); startTouched = true },
                    hint = "예) 25",
                    keyboardType = KeyboardType.Number,
                    // 해석 결과를 항상 명시 — "22"가 어느 달 22일인지 오해하지 않게
                    supportingText = when {
                        !startTouched -> "= ${currentCycle.start.korean()} (지금 기록된 ${paydayLabel})"
                        resolvedStart != null -> "= ${resolvedStart.korean()}로 기록돼요"
                        startDay != null -> "최근 두 달 안에 없는 날짜예요"
                        else -> "1~31 사이 숫자를 넣어주세요"
                    },
                    supportingTextColor = if (startTouched && resolvedStart != null) AmColors.Emerald else AmColors.TextTertiary,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(AmSpacing.lg))
                // 카드·행 수·설명 줄 수를 항상 고정 — 시간순(지난 → 이번)으로 읽히게 지난 회차를 위에.
                // "—" = 아직 값 없음, "없음 · 첫 회차예요" = 첫 회차보다 앞을 찍어 지난 회차가 구조적으로 없음.
                val p = startResult
                val previous = p?.previousCycle
                val firstCycle = p != null && previous == null
                PreviewCard(
                    rows = listOf(
                        "지난 회차" to when {
                            p == null -> DASH
                            previous == null -> NONE_FIRST_CYCLE
                            else -> periodText(previous, highlightStart = false, highlightEnd = true)
                        },
                        "이번 회차" to (p?.let { periodText(it.thisCycle, highlightStart = true, highlightEnd = false) } ?: DASH),
                    ),
                    note = if (firstCycle && startTouched) {
                        "첫 회차의 시작을 ${p!!.thisCycle.start.korean()}로 바꿔요. 매달 받는 날(${currentPayday.paydayText()})은 그대로예요."
                    } else {
                        "매달 받는 날(${currentPayday.paydayText()})은 그대로예요. 지난 회차는 받은 날 전날까지로 정리돼요."
                    },
                )
            }

            CycleEdge.END -> {
                Text("이번 ${paydayLabel}은 언제인가요?", style = AmType.size12_medium, color = AmColors.TextSecondary)
                Spacer(Modifier.height(AmSpacing.sm))
                AmLineTextField(
                    value = endInput,
                    onValueChange = { v -> endInput = v.filter { it.isDigit() }.take(2); endTouched = true },
                    hint = "예) 20",
                    keyboardType = KeyboardType.Number,
                    supportingText = when {
                        !endTouched -> "= ${currentCycle.endExclusive.korean()} (지금 잡혀 있는 ${paydayLabel})"
                        resolvedEnd != null -> "= ${resolvedEnd.korean()}에 받는 걸로 계산해요"
                        endDay != null -> "다가오는 두 달 안에 없는 날짜예요"
                        else -> "1~31 사이 숫자를 넣어주세요"
                    },
                    supportingTextColor = if (endTouched && resolvedEnd != null) AmColors.Emerald else AmColors.TextTertiary,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(AmSpacing.lg))
                val end = resolvedEnd
                val next = nextAfterEnd?.takeIf { it.start == end }   // 입력이 바뀐 직후 옛 계산이 잠깐 남는 것 방지
                PreviewCard(
                    rows = listOf(
                        "이번 회차" to (end?.let { periodText(BudgetCycle(currentCycle.start, it), highlightStart = false, highlightEnd = endTouched) } ?: DASH),
                        // 다음 회차도 기간으로 — 시작은 고른 날, 끝은 규칙일(주말·공휴일 보정)로 계산된 값
                        "다음 회차" to (next?.let { periodText(it, highlightStart = endTouched, highlightEnd = false) } ?: DASH),
                    ),
                    note = if (endTouched && end != null) {
                        // 회차는 그 전날에 끝나므로 "끝나요"가 아니라 "받아요"로 — 숫자의 뜻(월급일)과 맞춘다
                        "이번 달만 ${end.korean()}에 받아요. 다음부터는 다시 매월 ${currentPayday.paydayText()}이에요."
                    } else {
                        "매달 받는 날(${currentPayday.paydayText()}) 그대로예요. 이번 달만 다른 날 받으면 위에 적어주세요."
                    },
                )
            }
        }
    }
}

// ─────────────────────────── 공용 ───────────────────────────

/** 값이 아직 없을 때의 자리표시 — 카드 높이를 유지한다 */
private val DASH = androidx.compose.ui.text.AnnotatedString("—")

/** 지난 회차가 구조적으로 없을 때(첫 회차보다 앞을 찍음) — "아직 없음"과 구분되게 말로 적는다 */
private val NONE_FIRST_CYCLE = buildAnnotatedString {
    withStyle(SpanStyle(color = AmColors.TextTertiary)) { append("없음 · 첫 회차예요") }
}

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
