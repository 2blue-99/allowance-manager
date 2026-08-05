package com.allowance.manager.core.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmType
import java.time.YearMonth

/**
 * 월 네비게이션 바 (‹ 2026년 7월 ▾ ›). 월별·통계 화면 공용.
 * 이동 폭(월별=1달 / 통계=3달 등)은 [onPrev]/[onNext] 콜백에서 호출부가 결정한다.
 * 좌우 여백·상하 여백은 [modifier]로 준다(카드 안/밖 상황에 맞춰).
 */
@Composable
fun MonthNavBar(
    month: YearMonth,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPickMonth: () -> Unit,
    modifier: Modifier = Modifier,
    prevDesc: String = "이전",
    nextDesc: String = "다음",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavArrow(icon = Icons.Filled.ChevronLeft, enabled = canGoPrev, contentDescription = prevDesc, onClick = onPrev)
        Row(
            modifier = Modifier
                .clip(AmShape.pill)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPickMonth,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${month.year}년 ${month.monthValue}월", style = AmType.header, color = AmColors.TextPrimary)
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "월 선택",
                tint = AmColors.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
        NavArrow(icon = Icons.Filled.ChevronRight, enabled = canGoNext, contentDescription = nextDesc, onClick = onNext)
    }
}

/**
 * 월 선택 피커 다이얼로그 (연도 이동 + 3열 x 4행 월 그리드). 월별·통계 공용.
 * @param dataMonths 거래가 있는 달 집합. 이 달들(+현재 선택월)만 선택 가능, 나머지는 회색 비활성.
 */
@Composable
fun MonthPickerDialog(
    current: YearMonth,
    minMonth: YearMonth?,
    maxMonth: YearMonth,
    dataMonths: Set<YearMonth>,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var year by remember { mutableStateOf(current.year) }
    val minYear = minMonth?.year ?: (maxMonth.year - 10)
    val canPrevYear = year > minYear
    val canNextYear = year < maxMonth.year

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(AmShape.cardLarge)
                .background(AmColors.CardBg)
                .padding(20.dp),
        ) {
            // 연도 네비게이션
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavArrow(Icons.Filled.ChevronLeft, canPrevYear, "이전 해") { if (canPrevYear) year-- }
                Text("${year}년", style = AmType.title, color = AmColors.TextPrimary)
                NavArrow(Icons.Filled.ChevronRight, canNextYear, "다음 해") { if (canNextYear) year++ }
            }
            // 3열 x 4행 월 그리드
            (0 until 4).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    (0 until 3).forEach { col ->
                        val m = row * 3 + col + 1
                        val ym = YearMonth.of(year, m)
                        // 데이터 있는 달(+현재 선택월)만 선택 가능, 빈 달은 회색 비활성.
                        // dataMonths가 비면(정보 없음/디버그 등) 기존 [minMonth, maxMonth] 범위 방식으로 폴백.
                        val enabled = if (dataMonths.isEmpty()) {
                            ym >= (minMonth ?: ym) && ym <= maxMonth
                        } else {
                            ym == current || ym in dataMonths
                        }
                        MonthCell(
                            label = "${m}월",
                            selected = ym == current,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(ym) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavArrow(
    icon: ImageVector,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(AmShape.pill)
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) AmColors.TextPrimary else AmColors.BarTrack,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun MonthCell(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = when {
        selected -> AmColors.Emerald
        else -> Color.Transparent
    }
    val fg = when {
        !enabled -> AmColors.BarTrack
        selected -> Color.White
        else -> AmColors.TextPrimary
    }
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(46.dp)
            .clip(AmShape.card)
            .background(bg)
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = AmType.bodyStrong, color = fg)
    }
}
