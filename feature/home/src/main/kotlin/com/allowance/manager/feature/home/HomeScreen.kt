package com.allowance.manager.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.domain.model.Spending
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.ui.theme.AmStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ── 색상 ──────────────────────────────────────────────
private val HeroBg        = Color(0xFF0D1B2A)
private val AccentGreen   = Color(0xFF10B981)
private val AccentBg      = Color(0xFFECFDF5)
private val BottomBg      = Color(0xFFF0F2F6)
private val TextPrimary   = Color(0xFF0D1B2A)
private val TextSecondary = Color(0xFF8A97AA)
private val CardBg        = Color.White
private val SpendRed      = Color(0xFFEF4444)
private val NavBorder     = Color(0xFFEEF1F6)
private val PillBg        = Color(0x0FFFFFFF)
private val PillDivider   = Color(0x0FFFFFFF)
private val RingTrack     = Color(0x12FFFFFF)
private val IconBtnBg     = Color(0x14FFFFFF)

@Composable
fun HomeRoute(
    onNavigateToSetting: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onNavigateToSetting = onNavigateToSetting,
        onSaveMonthAllowance = viewModel::saveMonthAllowance,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNavigateToSetting: () -> Unit = {},
    onSaveMonthAllowance: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        HeroSection(
            uiState = uiState,
            onNavigateToSetting = onNavigateToSetting,
            onSaveMonthAllowance = onSaveMonthAllowance,
        )
        BottomContent(uiState = uiState, modifier = Modifier.weight(1f))
        BottomNav(onNavigateToSetting = onNavigateToSetting)
    }
}

// ── 상단 히어로 ─────────────────────────────────────────
@Composable
private fun HeroSection(
    uiState: HomeUiState,
    onNavigateToSetting: () -> Unit,
    onSaveMonthAllowance: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputAmount by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HeroBg)
            .padding(horizontal = 22.dp)
            .padding(bottom = 20.dp),
    ) {
        // 상단 타이틀 + 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "용돈 관리",
                style = AmStyle.text18.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconBtn(label = "🔔")
                IconBtn(label = "⚙️", onClick = onNavigateToSetting)
            }
        }

        // 링 차트
        RingChart(
            dailyRemaining = uiState.dailyRemaining,
            dailyAllowance = uiState.dailyAllowance,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Row {
            TextField(
                value = inputAmount,
                onValueChange = { inputAmount = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(onClick = {
                val amount = inputAmount.toLongOrNull() ?: return@Button
                onSaveMonthAllowance(amount)
                inputAmount = ""
            }) {
                Text(text = "저장")
            }
        }


        // 하단 3개 필
        StatsPills(uiState = uiState)
    }
}

@Composable
private fun IconBtn(
    label: String,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(50))
            .background(IconBtnBg),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 13.sp)
    }
}

@Composable
private fun RingChart(
    dailyRemaining: Long,
    dailyAllowance: Long,
    modifier: Modifier = Modifier,
) {
    val percent = if (dailyAllowance > 0) {
        (dailyRemaining.toFloat() / dailyAllowance.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val ringSize = 148.dp
    val strokeWidth = 13.dp

    Box(
        modifier = modifier
            .size(ringSize)
            .padding(bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Canvas 링
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // 배경 트랙
            drawArc(
                color = RingTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            // 진행 아크
            drawArc(
                color = AccentGreen,
                startAngle = -90f,
                sweepAngle = percent * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }

        // 중앙 텍스트
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "오늘 남은 금액",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 0.5.sp,
            )
            Text(
                text = "₩${dailyRemaining.amountToComma()}",
                style = AmStyle.text24.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp,
                ),
                color = Color.White,
            )
            Text(
                text = "${(percent * 100).toInt()}% 남음",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGreen,
            )
        }
    }
}

@Composable
private fun StatsPills(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PillBg),
    ) {
        PillItem(
            value = "${uiState.monthRemaining.amountToComma()}원",
            label = "이달 잔액",
            modifier = Modifier.weight(1f),
        )
        Box(modifier = Modifier.width(1.dp).height(44.dp).background(PillDivider))
        PillItem(
            value = "D-${uiState.daysUntilPayday}",
            label = "월급까지",
            valueColor = AccentGreen,
            modifier = Modifier.weight(1f),
        )
        Box(modifier = Modifier.width(1.dp).height(44.dp).background(PillDivider))
        PillItem(
            value = "${uiState.dailyAllowance.amountToComma()}원",
            label = "하루 용돈",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PillItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
) {
    Column(
        modifier = modifier.padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
        )
        Text(
            text = label,
            fontSize = 8.sp,
            color = Color.White.copy(alpha = 0.35f),
        )
    }
}

// ── 하단 컨텐츠 ──────────────────────────────────────────
@Composable
private fun BottomContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(BottomBg),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item { BudgetProgressCard(uiState = uiState) }
            item { TodaySpendingCard(spendings = uiState.todaySpendings) }
        }
    }
}

@Composable
private fun BudgetProgressCard(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    val percent = if (uiState.monthAllowance > 0) {
        (uiState.monthSpent.toFloat() / uiState.monthAllowance.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "이번달 예산",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentBg)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "${(percent * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentGreen,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BottomBg),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentGreen),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${uiState.monthSpent.amountToComma()}원 사용",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGreen,
            )
            Text(
                text = "${uiState.monthAllowance.amountToComma()}원 중",
                fontSize = 10.sp,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun TodaySpendingCard(
    spendings: List<Spending>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "오늘 소비",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
            )
            Text(
                text = "전체 →",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGreen,
            )
        }

        if (spendings.isEmpty()) {
            Text(
                text = "오늘 소비 내역이 없습니다",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        } else {
            spendings.forEachIndexed { index, spending ->
                SpendingItem(
                    spending = spending,
                    showDivider = index < spendings.lastIndex,
                )
            }
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun SpendingItem(
    spending: Spending,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val isSpend = spending.type == TransactionType.SPEND
    val amountColor = if (isSpend) SpendRed else AccentGreen
    val amountText = if (isSpend) "-${spending.amount.amountToComma()}원" else "+${spending.amount.amountToComma()}원"
    val time = Instant.ofEpochMilli(spending.timestamp)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(AccentBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = if (isSpend) "💳" else "💰", fontSize = 14.sp)
                }
                Column {
                    Text(
                        text = spending.memo.ifBlank { if (isSpend) "카드 결제" else "입금" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = time,
                        fontSize = 9.sp,
                        color = Color(0xFFA0AABB),
                    )
                }
            }
            Text(
                text = amountText,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor,
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF4F6FA)),
            )
        }
    }
}

// ── 하단 네비게이션 ───────────────────────────────────────
@Composable
private fun BottomNav(
    onNavigateToSetting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .background(CardBg)
            .drawBehind {
                drawLine(
                    color = NavBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(top = 8.dp),
    ) {
        NavItem(icon = "🏠", label = "홈", isSelected = true, modifier = Modifier.weight(1f))
        NavItem(icon = "📋", label = "내역", modifier = Modifier.weight(1f))
        NavItem(icon = "📊", label = "분석", modifier = Modifier.weight(1f))
        NavItem(icon = "⚙️", label = "설정", modifier = Modifier.weight(1f), onClick = onNavigateToSetting)
    }
}

@Composable
private fun NavItem(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) AccentGreen else Color(0xFFC0C8D8),
        )
    }
}

// ── Preview ──────────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        uiState = HomeUiState(
            dailyAllowance = 50_000L,
            dailyRemaining = 32_500L,
            monthAllowance = 300_000L,
            monthSpent = 186_500L,
            monthRemaining = 113_500L,
            daysUntilPayday = 8L,
            todaySpendings = listOf(
                Spending(id = 1, type = TransactionType.SPEND, amount = 6_500, timestamp = System.currentTimeMillis(), memo = "스타벅스"),
                Spending(id = 2, type = TransactionType.SPEND, amount = 1_400, timestamp = System.currentTimeMillis() - 3_600_000, memo = ""),
                Spending(id = 3, type = TransactionType.SPEND, amount = 9_600, timestamp = System.currentTimeMillis() - 7_200_000, memo = "편의점"),
            ),
        ),
    )
}
