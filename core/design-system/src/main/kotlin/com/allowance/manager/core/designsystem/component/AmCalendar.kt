package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** 요일 머리글 — 일요일 시작 */
private val WEEK_HEADERS = listOf("일", "월", "화", "수", "목", "금", "토")

/**
 * 한 달 날짜 그리드. 날짜를 고르고, 특정 구간을 음영으로 표시할 수 있다.
 *
 * @param month 보여줄 달. 위 화살표로 [onMonthChange]를 통해 바꾼다.
 * @param selected 선택된 날짜 (진한 칸)
 * @param highlight 음영으로 감쌀 구간. 사이클 경계를 눈으로 보여줄 때 쓴다.
 * @param selectableRange 고를 수 있는 범위. 벗어난 날짜는 흐리게 표시되고 눌리지 않는다.
 */
@Composable
fun AmCalendar(
    month: YearMonth,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    highlight: ClosedRange<LocalDate>? = null,
    selectableRange: ClosedRange<LocalDate>? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 달 이동 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalendarArrow(Icons.Filled.ChevronLeft, "이전 달") { onMonthChange(month.minusMonths(1)) }
            Text(
                "${month.year}년 ${month.monthValue}월",
                style = AmType.size14_bold,
                color = AmColors.TextPrimary,
            )
            CalendarArrow(Icons.Filled.ChevronRight, "다음 달") { onMonthChange(month.plusMonths(1)) }
        }
        Spacer(Modifier.height(AmSpacing.sm))

        Row(Modifier.fillMaxWidth()) {
            WEEK_HEADERS.forEach { label ->
                Text(
                    label,
                    style = AmType.size10_medium,
                    color = AmColors.TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(AmSpacing.xs))

        // 1일이 놓일 칸 수만큼 앞을 비운다 (일요일 시작)
        val firstDay = month.atDay(1)
        val leading = firstDay.dayOfWeek.sundayIndex()
        val cells = leading + month.lengthOfMonth()
        val rows = (cells + 6) / 7

        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val dayNumber = row * 7 + col - leading + 1
                    val date = if (dayNumber in 1..month.lengthOfMonth()) month.atDay(dayNumber) else null
                    DayCell(
                        date = date,
                        selected = date != null && date == selected,
                        inHighlight = date != null && highlight?.contains(date) == true,
                        enabled = date != null && (selectableRange == null || selectableRange.contains(date)),
                        onClick = { date?.let(onSelect) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** 일요일을 0으로 두는 인덱스 — 그리드가 일요일부터 시작하므로 */
private fun DayOfWeek.sundayIndex(): Int = value % 7

@Composable
private fun DayCell(
    date: LocalDate?,
    selected: Boolean,
    inHighlight: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(38.dp)
            // 음영은 칸 전체를 채워 구간이 이어져 보이게 한다 (선택 칸만 둥글게)
            .background(if (inHighlight && !selected) AmColors.CalendarRangeBg else Color.Transparent)
            .clip(if (selected) AmShape.cardSmall else RoundedCornerShape(0.dp))
            .background(if (selected) AmColors.Navy else Color.Transparent)
            .clickable(
                enabled = date != null && enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Text(
                date.dayOfMonth.toString(),
                style = if (selected) AmType.size12_bold else AmType.size12_medium,
                color = when {
                    selected -> Color.White
                    !enabled -> AmColors.TextTertiary.copy(alpha = 0.4f)
                    else -> AmColors.TextPrimary
                },
            )
        }
    }
}

@Composable
private fun CalendarArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(AmShape.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(AmSpacing.sm),
    ) {
        Icon(icon, contentDescription = description, tint = AmColors.TextTertiary, modifier = Modifier.size(20.dp))
    }
}
