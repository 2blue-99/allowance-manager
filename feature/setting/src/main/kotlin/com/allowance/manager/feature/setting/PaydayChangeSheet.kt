package com.allowance.manager.feature.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmCalendar
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.AmLineTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.lastDay
import java.time.LocalDate
import java.time.YearMonth

/** 말일 — payday 0은 매월 마지막 날로 clamp된다 */
private const val PAYDAY_EOM = 0

/**
 * 월급일 변경 시트.
 *
 * 두 값을 함께 정한다.
 * - **규칙일**(칩·직접 입력): 매달 반복되는 날
 * - **경계일**(캘린더): 이번 변경이 시작되는 날 = "이 날 받았다"
 *
 * 규칙일을 고르면 캘린더가 그 날짜를 자동 선택하고, 실제로 다른 날 받았으면 캘린더에서 덮어쓴다.
 * 경계가 하나뿐이라 사이클 사이에 빈틈이나 겹침이 생길 수 없다.
 *
 * @param currentCycle 지금 보고 있는 사이클 — 캘린더에 음영으로 표시하고 요약에 쓴다
 * @param onSave (경계일, 규칙일)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaydayChangeSheet(
    currentCycle: BudgetCycle,
    currentPayday: Int,
    title: String,
    onSave: (LocalDate, Int) -> Unit,
    onDismiss: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 규칙일 — 말일은 대표값 31로 보여주되 저장은 EOM으로
    var input by remember { mutableStateOf(if (currentPayday in 1..31) currentPayday.toString() else "31") }
    var isEom by remember { mutableStateOf(currentPayday == PAYDAY_EOM) }
    val day = input.toIntOrNull()?.takeIf { it in 1..31 }
    val payday = if (isEom) PAYDAY_EOM else day

    // 경계일 — 처음엔 지금 사이클 시작일(= 마지막으로 받은 날)
    var boundary by remember { mutableStateOf(currentCycle.start) }
    var calendarMonth by remember { mutableStateOf(YearMonth.from(currentCycle.start)) }

    // 전달·이번달·다음달 3개월까지 고를 수 있다. 알림으로 자동 기록되는 앱이라
    // 어긋난 걸 나중에 발견하는 경우가 많아 과거 수정을 열어둔다.
    val selectableRange = remember(today) {
        YearMonth.from(today).minusMonths(1).atDay(1)..YearMonth.from(today).plusMonths(1).atEndOfMonth()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmSpacing.xl)
                .padding(bottom = AmSpacing.xxl),
        ) {
            Text(title, style = AmType.size18_black, color = AmColors.TextPrimary)

            Spacer(Modifier.height(AmSpacing.md))
            // 고르는 동안 결과가 계속 보이도록 요약을 위에 둔다
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AmShape.cardSmall)
                    .background(AmColors.ScreenBg)
                    .padding(AmSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AmSpacing.xs),
            ) {
                SummaryRow("이번 회차", "${boundary.periodText()} – ${currentCycle.lastDay.periodText()}")
                SummaryRow("다음부터", payday?.let { "매월 ${it.paydayText()}" } ?: "—")
            }

            Spacer(Modifier.height(AmSpacing.lg))
            Text("매달 받는 날", style = AmType.size12_medium, color = AmColors.TextSecondary)
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
            Text("이번에 받은 날", style = AmType.size12_medium, color = AmColors.TextSecondary)
            Spacer(Modifier.height(AmSpacing.xxs))
            Text(
                "실제로 받은 날이 다르면 눌러서 바꿔요",
                style = AmType.size10_medium,
                color = AmColors.TextTertiary,
            )
            Spacer(Modifier.height(AmSpacing.sm))
            AmCalendar(
                month = calendarMonth,
                selected = boundary,
                onSelect = { boundary = it },
                onMonthChange = { calendarMonth = it },
                highlight = boundary..currentCycle.lastDay.coerceAtLeast(boundary),
                selectableRange = selectableRange,
            )

            Spacer(Modifier.height(AmSpacing.lg))
            AmButton(
                text = "저장",
                onClick = { payday?.let { onSave(boundary, it) } },
                enabled = payday != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = AmType.size12_medium, color = AmColors.TextSecondary)
        Text(value, style = AmType.size12_bold, color = AmColors.TextPrimary)
    }
}

/** "8. 25" */
private fun LocalDate.periodText(): String = "$monthValue. $dayOfMonth"

/** "25일" / "말일" */
private fun Int.paydayText(): String = if (this <= 0) "말일" else "${this}일"

/** 경계일이 사이클 끝을 넘어서면(과거로 크게 당기면) 음영이 뒤집히지 않게 */
private fun LocalDate.coerceAtLeast(other: LocalDate): LocalDate = if (isBefore(other)) other else this
