package com.allowance.manager.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.util.amountToComma
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// ── 색상 ──────────────────────────────────────────────
private val HeroBg = Color(0xFF0D1B2A)
private val AccentGreen = Color(0xFF10B981)
private val AccentBg = Color(0xFFECFDF5)
private val BottomBg = Color(0xFFF0F2F6)
private val TextPrimary = Color(0xFF0D1B2A)
private val TextSecondary = Color(0xFF8A97AA)
private val CardBg = Color.White
private val SpendRed = Color(0xFFEF4444)
private val PillBg = Color(0x0FFFFFFF)
private val PillDivider = Color(0x14FFFFFF)
private val RingTrack = Color(0x12FFFFFF)
private val IconBtnBg = Color(0x14FFFFFF)
private val Divider = Color(0xFFF4F6FA)

@Composable
fun HomeRoute(
    onNavigateToSetting: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onNavigateToSetting = onNavigateToSetting,
        onToggleMainOnly = viewModel::onToggleMainOnly,
        onSetIgnored = viewModel::onSetIgnored,
        onDelete = viewModel::onDelete,
        onPromoteToMain = viewModel::onPromoteToMain,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNavigateToSetting: () -> Unit = {},
    onToggleMainOnly: () -> Unit = {},
    onSetIgnored: (Long, Boolean) -> Unit = { _, _ -> },
    onDelete: (Long) -> Unit = {},
    onPromoteToMain: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(BottomBg)) {
        Hero(uiState = uiState, onNavigateToSetting = onNavigateToSetting)
        BottomContent(
            uiState = uiState,
            onToggleMainOnly = onToggleMainOnly,
            onSetIgnored = onSetIgnored,
            onDelete = onDelete,
            onPromoteToMain = onPromoteToMain,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── 히어로 ───────────────────────────────────────────
@Composable
private fun Hero(uiState: HomeUiState, onNavigateToSetting: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeroBg)
            .padding(horizontal = 22.dp)
            .padding(top = 16.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("가계부", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            IconBtn(label = "⚙️", onClick = onNavigateToSetting)
        }

        BudgetRing(
            remaining = uiState.remaining,
            ratio = uiState.ratio,
            isOver = uiState.isOver,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        StatsPills(uiState = uiState)
    }
}

@Composable
private fun IconBtn(label: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(50))
            .background(IconBtnBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 13.sp)
    }
}

@Composable
private fun BudgetRing(remaining: Long, ratio: Float, isOver: Boolean, modifier: Modifier = Modifier) {
    val percent = ratio.coerceIn(0f, 1f)
    val ringColor = if (isOver) SpendRed else AccentGreen
    val ringSize = 148.dp
    val strokeWidth = 13.dp

    Box(
        modifier = modifier.size(ringSize).padding(bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(RingTrack, 0f, 360f, false, topLeft, arcSize, style = stroke)
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = if (isOver) 360f else percent * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "이번달 남은 금액",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 0.5.sp,
            )
            Text(
                text = if (isOver) "-${abs(remaining).amountToComma()}원" else "${remaining.amountToComma()}원",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.5).sp,
                color = if (isOver) SpendRed else Color.White,
            )
            Text(
                text = if (isOver) "예산 초과" else "${(percent * 100).toInt()}% 남음",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor,
            )
        }
    }
}

@Composable
private fun StatsPills(uiState: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PillBg),
    ) {
        PillItem("${uiState.spent.amountToComma()}원", "이번달 지출", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(44.dp).background(PillDivider))
        PillItem("${uiState.budget.amountToComma()}원", "월 예산", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(44.dp).background(PillDivider))
        PillItem(uiState.cycleLabel.substringAfter("다음 수급일 ").ifBlank { "-" }, "수급일까지", Modifier.weight(1f), AccentGreen)
    }
}

@Composable
private fun PillItem(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = Color.White) {
    Column(
        modifier = modifier.padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        Text(label, fontSize = 8.sp, color = Color.White.copy(alpha = 0.35f))
    }
}

// ── 하단 시트 ──────────────────────────────────────────
@Composable
private fun BottomContent(
    uiState: HomeUiState,
    onToggleMainOnly: () -> Unit,
    onSetIgnored: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onPromoteToMain: (Transaction) -> Unit,
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
            item { ListHeader(showMainOnly = uiState.showMainOnly, onToggleMainOnly = onToggleMainOnly) }

            if (uiState.transactions.isEmpty()) {
                item { EmptyState() }
            } else {
                items(uiState.transactions, key = { it.id }) { tx ->
                    TransactionCard(
                        tx = tx,
                        onSetIgnored = onSetIgnored,
                        onDelete = onDelete,
                        onPromoteToMain = onPromoteToMain,
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressCard(uiState: HomeUiState) {
    val percent = if (uiState.budget > 0) {
        (uiState.spent.toFloat() / uiState.budget).coerceIn(0f, 1f)
    } else 0f
    val barColor = if (uiState.isOver) SpendRed else AccentGreen
    val badgeBg = if (uiState.isOver) Color(0xFFFDECEC) else AccentBg

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg).padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("이번달 예산", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(badgeBg).padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    "${if (uiState.budget > 0) (uiState.spent * 100 / uiState.budget) else 0}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = barColor,
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(BottomBg),
        ) {
            Box(Modifier.fillMaxWidth(percent).height(7.dp).clip(RoundedCornerShape(4.dp)).background(barColor))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${uiState.spent.amountToComma()}원 사용", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = barColor)
            Text("${uiState.budget.amountToComma()}원 중", fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun ListHeader(showMainOnly: Boolean, onToggleMainOnly: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("이번달 내역", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text(
            text = if (showMainOnly) "전체 보기" else "메인만 보기",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGreen,
            modifier = Modifier.clickable(onClick = onToggleMainOnly),
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg).padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("이번달 내역이 없어요", fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun TransactionCard(
    tx: Transaction,
    onSetIgnored: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onPromoteToMain: (Transaction) -> Unit,
) {
    val dim = tx.isIgnored || !tx.isMain
    val isIncome = tx.type == TransactionType.INCOME || tx.amount < 0

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg).padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (dim) Divider else AccentBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (isIncome) "💰" else "💳", fontSize = 15.sp)
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            tx.sourceName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dim) TextSecondary else TextPrimary,
                        )
                        if (!tx.isMain) {
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Divider).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("미등록", fontSize = 8.sp, color = TextSecondary)
                            }
                        }
                    }
                    Text(formatTime(tx.createdAt), fontSize = 9.sp, color = Color(0xFFA0AABB))
                }
            }
            Text(
                text = signedAmount(tx),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor(tx, dim),
                textDecoration = if (tx.isIgnored) TextDecoration.LineThrough else TextDecoration.None,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!tx.isMain && tx.extractedAccount != null) {
                ActionText("메인 등록", AccentGreen) { onPromoteToMain(tx) }
            }
            ActionText(if (tx.isIgnored) "무시 취소" else "무시", TextSecondary) { onSetIgnored(tx.id, !tx.isIgnored) }
            ActionText("삭제", SpendRed) { onDelete(tx.id) }
        }
    }
}

@Composable
private fun ActionText(label: String, color: Color, onClick: () -> Unit) {
    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.clickable(onClick = onClick))
}

// ── helpers ──────────────────────────────────────────
private fun signedAmount(tx: Transaction): String {
    val magnitude = abs(tx.amount).amountToComma()
    return when {
        tx.type == TransactionType.INCOME -> "+${magnitude}원"
        tx.amount < 0 -> "+${magnitude}원"   // 취소·환불
        else -> "-${magnitude}원"
    }
}

private fun amountColor(tx: Transaction, dim: Boolean): Color = when {
    dim -> TextSecondary
    tx.type == TransactionType.INCOME || tx.amount < 0 -> AccentGreen
    else -> SpendRed
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

private fun formatTime(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDateTime().format(timeFormatter)
