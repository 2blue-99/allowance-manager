package com.allowance.manager.feature.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.ui.transaction.categoryColor
import com.allowance.manager.core.ui.transaction.categoryLabel
import java.time.YearMonth

@Composable
fun StatsRoute(
    onOpenMonth: (YearMonth) -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(
        uiState = uiState,
        onSelectMonth = viewModel::onSelectMonth,
        onOlder = viewModel::onOlder,
        onNewer = viewModel::onNewer,
        onOpenMonth = onOpenMonth,
    )
}

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onSelectMonth: (YearMonth) -> Unit = {},
    onOlder: () -> Unit = {},
    onNewer: () -> Unit = {},
    onOpenMonth: (YearMonth) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmColors.ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(AmSpacing.xl),
    ) {
        ChartCard(
            uiState = uiState,
            onSelectMonth = onSelectMonth,
            onOlder = onOlder,
            onNewer = onNewer,
        )
        Spacer(Modifier.height(AmSpacing.md))
        // 선택 월이 바뀌면 요약·파이를 통째로 페이드 인/아웃 (같은 달 데이터 갱신엔 애니메이션 없음)
        AnimatedContent(
            targetState = MonthDetail(uiState.selected, uiState.summary, uiState.categories),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            contentKey = { it.month },
            label = "statsDetail",
        ) { detail ->
            Column {
                SummaryCard(
                    month = detail.month,
                    summary = detail.summary,
                    label = uiState.userType.label,
                    onOpenMonth = { onOpenMonth(detail.month) },
                )
                Spacer(Modifier.height(AmSpacing.md))
                CategoryCard(month = detail.month, categories = detail.categories, total = detail.summary.expense)
            }
        }
        Spacer(Modifier.height(AmSpacing.md))
    }
}

/** 선택 월의 요약·분류 스냅샷 — 월 변경 시 이전/새 데이터를 각각 잡아 페이드 크로스 처리 */
private data class MonthDetail(
    val month: java.time.YearMonth,
    val summary: MonthSummary,
    val categories: List<CategorySlice>,
)

// ── 6개월 막대 차트 (지출 막대 + 계단식 용돈 점선) ──
@Composable
private fun ChartCard(
    uiState: StatsUiState,
    onSelectMonth: (YearMonth) -> Unit,
    onOlder: () -> Unit,
    onNewer: () -> Unit,
) {
    AmCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavArrow(Icons.Filled.ChevronLeft, uiState.canOlder, "이전 3개월", onOlder)
                Text(uiState.rangeLabel, style = AmType.labelStrong, color = AmColors.TextPrimary)
                NavArrow(Icons.Filled.ChevronRight, uiState.canNewer, "다음 3개월", onNewer)
            }

            Spacer(Modifier.height(18.dp))
            BarChart(bars = uiState.window, onSelectMonth = onSelectMonth)

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                LegendDot(AmColors.Emerald, "${uiState.userType.label} 이하")
                LegendDot(AmColors.Red, "${uiState.userType.label} 초과")
                LegendDash("${uiState.userType.label}(월별)")
            }
        }
    }
}

private val CHART_HEIGHT = 150.dp
private val CHART_TOP_RESERVE = 18.dp   // 선택 달 금액 라벨 공간

@Composable
private fun BarChart(bars: List<MonthBar>, onSelectMonth: (YearMonth) -> Unit) {
    val density = LocalDensity.current
    // 스케일: 지출·용돈 중 최대값 + 15% 여유
    val maxValue = (bars.maxOfOrNull { maxOf(it.expense, it.budget) } ?: 1L)
        .coerceAtLeast(1L)
        .let { (it * 1.15f) }
    val barMaxHeight = CHART_HEIGHT - CHART_TOP_RESERVE

    Box(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
        // 막대 (Composable)
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
            bars.forEach { bar ->
                val fraction = (bar.expense / maxValue).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .then(if (bar.isSelected) Modifier.background(AmColors.ScreenBg) else Modifier)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelectMonth(bar.yearMonth) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (bar.isSelected) {
                        Text(
                            manWon(bar.expense),
                            style = AmType.microStrong,
                            color = if (bar.isOver) AmColors.Red else AmColors.Emerald,
                        )
                        Spacer(Modifier.height(3.dp))
                    }
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(barMaxHeight * fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (bar.isOver) AmColors.Red else AmColors.Emerald),
                    )
                }
            }
        }

        // 계단식 용돈 점선 (Canvas 오버레이)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val reservePx = with(density) { CHART_TOP_RESERVE.toPx() }
            val barMaxPx = size.height - reservePx
            val colW = size.width / bars.size
            val dash = PathEffect.dashPathEffect(floatArrayOf(9f, 9f))
            fun yFor(budget: Long) = size.height - (budget / maxValue).coerceIn(0f, 1f) * barMaxPx

            bars.forEachIndexed { i, bar ->
                if (bar.budget <= 0L) return@forEachIndexed
                val y = yFor(bar.budget)
                drawLine(
                    color = AmColors.TextTertiary,
                    start = Offset(i * colW, y),
                    end = Offset((i + 1) * colW, y),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = dash,
                    cap = StrokeCap.Round,
                )
                // 이전 칸과 용돈 레벨이 다르면 경계에 수직 연결선
                val prev = bars.getOrNull(i - 1)
                if (prev != null && prev.budget > 0L && prev.budget != bar.budget) {
                    drawLine(
                        color = AmColors.TextTertiary,
                        start = Offset(i * colW, yFor(prev.budget)),
                        end = Offset(i * colW, y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = dash,
                    )
                }
            }
        }

    }

    Spacer(Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        bars.forEach { bar ->
            Text(
                bar.label,
                style = AmType.micro,
                color = if (bar.isSelected) AmColors.TextPrimary else AmColors.TextSecondary,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ── 선택 달 요약 (용돈/지출/수입) ──
@Composable
private fun SummaryCard(month: YearMonth, summary: MonthSummary, label: String, onOpenMonth: () -> Unit) {
    AmCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${month.monthValue}월 요약", style = AmType.labelStrong, color = AmColors.TextPrimary)
                if (summary.isOver) {
                    Text(
                        "  ·  $label 초과 ${summary.over.amountToComma()}원",
                        style = AmType.captionStrong,
                        color = AmColors.Red,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SummaryCell(label, "${summary.budget.amountToComma()}원", AmColors.TextPrimary, Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(34.dp).background(AmColors.Divider))
                SummaryCell(
                    "지출",
                    "${summary.expense.amountToComma()}원",
                    if (summary.isOver) AmColors.Red else AmColors.Emerald,
                    Modifier.weight(1f),
                )
                Box(Modifier.width(1.dp).height(34.dp).background(AmColors.Divider))
                SummaryCell("수입", "${summary.income.amountToComma()}원", AmColors.Emerald, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmColors.EmeraldBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenMonth,
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("이 달 내역 보기 ›", style = AmType.captionStrong, color = AmColors.Emerald)
            }
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = AmType.micro, color = AmColors.TextSecondary)
        Spacer(Modifier.height(5.dp))
        Text(value, style = AmType.valueStrong, color = valueColor)
    }
}

// ── 카테고리 도넛 + 랭킹 ──
@Composable
private fun CategoryCard(month: YearMonth, categories: List<CategorySlice>, total: Long) {
    AmCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("${month.monthValue}월 카테고리 분포", style = AmType.labelStrong, color = AmColors.TextPrimary)
            Spacer(Modifier.height(16.dp))
            if (categories.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    Text("이 달 지출 내역이 없어요", style = AmType.caption, color = AmColors.TextSecondary)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Donut(categories = categories, total = total, modifier = Modifier.size(120.dp))
                    Spacer(Modifier.width(18.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        categories.forEach { slice ->
                            CategoryRow(slice)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Donut(categories: List<CategorySlice>, total: Long, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.22f
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            var start = -90f
            categories.forEach { slice ->
                val sweep = slice.ratio * 360f
                drawArc(
                    color = categoryColor(slice.category),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("총지출", style = AmType.tiny, color = AmColors.TextSecondary)
            Text(manWon(total), style = AmType.valueStrong, color = AmColors.TextPrimary)
        }
    }
}

@Composable
private fun CategoryRow(slice: CategorySlice) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(categoryColor(slice.category)))
        Spacer(Modifier.width(6.dp))
        Text(categoryLabel(slice.category), style = AmType.caption, color = AmColors.TextPrimary)
        Spacer(Modifier.width(6.dp))
        Text("${(slice.ratio * 100).toInt()}%", style = AmType.caption, color = AmColors.TextTertiary)
        Spacer(Modifier.weight(1f))
        Text("${slice.amount.amountToComma()}원", style = AmType.captionStrong, color = AmColors.TextPrimary)
    }
}

// ── 공통 소품 ──
@Composable
private fun NavArrow(icon: ImageVector, enabled: Boolean, cd: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
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
        Icon(icon, contentDescription = cd, tint = if (enabled) AmColors.TextPrimary else AmColors.BarTrack, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = AmType.micro, color = AmColors.TextSecondary)
    }
}

@Composable
private fun LegendDash(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(12.dp).height(2.dp).background(AmColors.TextTertiary))
        Spacer(Modifier.width(4.dp))
        Text(label, style = AmType.micro, color = AmColors.TextSecondary)
    }
}

private fun manWon(amount: Long): String =
    if (amount >= 10_000L) "${amount / 10_000L}만" else amount.amountToComma()
