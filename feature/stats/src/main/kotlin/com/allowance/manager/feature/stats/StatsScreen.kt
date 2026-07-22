package com.allowance.manager.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmScreenHeader
import com.allowance.manager.core.designsystem.component.AmStatusChip
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.domain.util.amountToComma
import kotlin.math.abs

@Composable
fun StatsRoute(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(uiState = uiState)
}

@Composable
fun StatsScreen(uiState: StatsUiState) {
    Column(
        modifier = Modifier.fillMaxSize().background(AmColors.ScreenBg).padding(AmSpacing.xl),
    ) {
        AmScreenHeader(title = "통계")
        Spacer(Modifier.height(AmSpacing.xl))
        SummaryCard(currentMonthTotal = uiState.currentMonthTotal, prevMonthDiff = uiState.prevMonthDiff)
        Spacer(Modifier.height(AmSpacing.md))
        BarChartCard(bars = uiState.bars)
    }
}

@Composable
private fun SummaryCard(currentMonthTotal: Long, prevMonthDiff: Long) {
    AmCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("이번달 지출", fontSize = 12.sp, color = AmColors.TextSecondary)
            Spacer(Modifier.height(AmSpacing.xs + 2.dp))
            Text("${currentMonthTotal.amountToComma()}원", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AmColors.TextPrimary)
            Spacer(Modifier.height(AmSpacing.sm + 2.dp))

            val (arrow, color, chipBg) = when {
                prevMonthDiff > 0 -> Triple("▲", AmColors.Red, AmColors.RedBg)
                prevMonthDiff < 0 -> Triple("▼", AmColors.Emerald, AmColors.EmeraldBg)
                else -> Triple("–", AmColors.TextSecondary, AmColors.ScreenBg)
            }
            AmStatusChip(
                text = if (prevMonthDiff == 0L) "지난달과 동일" else "지난달 대비 $arrow ${abs(prevMonthDiff).amountToComma()}원",
                color = color,
                background = chipBg,
            )
        }
    }
}

@Composable
private fun BarChartCard(bars: List<MonthBar>) {
    AmCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("최근 6개월", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = AmColors.TextPrimary)
            Spacer(Modifier.height(AmSpacing.lg + 2.dp))

            val max = (bars.maxOfOrNull { it.total } ?: 0L).coerceAtLeast(1L)
            val chartHeight = 130.dp

            Row(
                modifier = Modifier.fillMaxWidth().height(chartHeight + 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEach { bar ->
                    val fraction = (bar.total.toFloat() / max).coerceIn(0f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (bar.total > 0) "${bar.total / 10000}만" else "",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bar.isCurrent) AmColors.Emerald else AmColors.TextSecondary,
                        )
                        Spacer(Modifier.height(AmSpacing.xs))
                        Box(
                            modifier = Modifier.width(22.dp).height(chartHeight),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height((chartHeight) * fraction)
                                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                    .background(if (bar.isCurrent) AmColors.Emerald else AmColors.BarTrack),
                            )
                        }
                        Spacer(Modifier.height(AmSpacing.sm))
                        Text(
                            bar.label,
                            fontSize = 10.sp,
                            color = if (bar.isCurrent) AmColors.TextPrimary else AmColors.TextSecondary,
                            fontWeight = if (bar.isCurrent) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
