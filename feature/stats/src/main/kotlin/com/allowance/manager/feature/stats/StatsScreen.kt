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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.domain.util.amountToComma
import kotlin.math.abs

private val Accent = Color(0xFF10B981)
private val AccentBg = Color(0xFFECFDF5)
private val BarTrack = Color(0xFFE3E7EE)
private val Bg = Color(0xFFF0F2F6)
private val TextPrimary = Color(0xFF0D1B2A)
private val TextSecondary = Color(0xFF8A97AA)
private val UpRed = Color(0xFFEF4444)
private val UpRedBg = Color(0xFFFDECEC)

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
        modifier = Modifier.fillMaxSize().background(Bg).padding(20.dp),
    ) {
        Text("통계", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(Modifier.height(20.dp))
        SummaryCard(currentMonthTotal = uiState.currentMonthTotal, prevMonthDiff = uiState.prevMonthDiff)
        Spacer(Modifier.height(12.dp))
        BarChartCard(bars = uiState.bars)
    }
}

@Composable
private fun SummaryCard(currentMonthTotal: Long, prevMonthDiff: Long) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(18.dp),
    ) {
        Text("이번달 지출", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Text("${currentMonthTotal.amountToComma()}원", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(Modifier.height(10.dp))

        val (arrow, color, chipBg) = when {
            prevMonthDiff > 0 -> Triple("▲", UpRed, UpRedBg)
            prevMonthDiff < 0 -> Triple("▼", Accent, AccentBg)
            else -> Triple("–", TextSecondary, Bg)
        }
        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(chipBg).padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(
                text = if (prevMonthDiff == 0L) "지난달과 동일" else "지난달 대비 $arrow ${abs(prevMonthDiff).amountToComma()}원",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun BarChartCard(bars: List<MonthBar>) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(18.dp),
    ) {
        Text("최근 6개월", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(Modifier.height(18.dp))

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
                        color = if (bar.isCurrent) Accent else TextSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.width(22.dp).height(chartHeight),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((chartHeight) * fraction)
                                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                .background(if (bar.isCurrent) Accent else BarTrack),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        bar.label,
                        fontSize = 10.sp,
                        color = if (bar.isCurrent) TextPrimary else TextSecondary,
                        fontWeight = if (bar.isCurrent) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
