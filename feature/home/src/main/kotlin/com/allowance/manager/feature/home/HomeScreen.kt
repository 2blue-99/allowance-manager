package com.allowance.manager.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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

private val HeroBg = Color(0xFF0D1B2A)
private val AccentGreen = Color(0xFF10B981)
private val SpendRed = Color(0xFFEF4444)
private val BottomBg = Color(0xFFF0F2F6)
private val TextPrimary = Color(0xFF0D1B2A)
private val TextSecondary = Color(0xFF8A97AA)
private val RingTrack = Color(0x22FFFFFF)

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
        ListHeader(showMainOnly = uiState.showMainOnly, onToggleMainOnly = onToggleMainOnly)
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            items(uiState.transactions, key = { it.id }) { tx ->
                TransactionRow(
                    tx = tx,
                    onSetIgnored = onSetIgnored,
                    onDelete = onDelete,
                    onPromoteToMain = onPromoteToMain,
                )
            }
        }
    }
}

@Composable
private fun Hero(uiState: HomeUiState, onNavigateToSetting: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeroBg)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("가계부", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "⚙",
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.size(24.dp).clickable(onClick = onNavigateToSetting),
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            BudgetRing(ratio = uiState.ratio, isOver = uiState.isOver)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("이번달 남은 금액", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                val remainingText =
                    if (uiState.isOver) "-${abs(uiState.remaining).amountToComma()}원 초과"
                    else "${uiState.remaining.amountToComma()}원"
                Text(
                    remainingText,
                    color = if (uiState.isOver) SpendRed else Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x0FFFFFFF))
                .padding(vertical = 10.dp),
        ) {
            Pill("이번달 지출", "${uiState.spent.amountToComma()}원", Modifier.weight(1f))
            Pill("월 예산", "${uiState.budget.amountToComma()}원", Modifier.weight(1f))
            Pill(uiState.cycleLabel.ifBlank { "수급일" }, "", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Pill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (value.isNotBlank()) {
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
        }
        Text(label, color = TextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun BudgetRing(ratio: Float, isOver: Boolean) {
    val sweep = (ratio.coerceIn(0f, 1f)) * 360f
    val color = if (isOver) SpendRed else AccentGreen
    Box(
        modifier = Modifier
            .size(148.dp)
            .drawBehind {
                val stroke = 13.dp.toPx()
                drawArc(
                    color = RingTrack,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = if (isOver) 360f else sweep,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            },
    )
}

@Composable
private fun ListHeader(showMainOnly: Boolean, onToggleMainOnly: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("이번달 내역", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onToggleMainOnly) {
            Text(if (showMainOnly) "전체 보기" else "메인만 보기", fontSize = 12.sp, color = AccentGreen)
        }
    }
}

@Composable
private fun TransactionRow(
    tx: Transaction,
    onSetIgnored: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onPromoteToMain: (Transaction) -> Unit,
) {
    val dim = tx.isIgnored || !tx.isMain
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.sourceName,
                    color = if (dim) TextSecondary else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(formatTime(tx.createdAt), color = TextSecondary, fontSize = 10.sp)
            }
            Text(
                text = signedAmount(tx),
                color = amountColor(tx, dim),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (tx.isIgnored) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!tx.isMain && tx.extractedAccount != null) {
                TextButton(onClick = { onPromoteToMain(tx) }) {
                    Text("메인 등록", fontSize = 11.sp, color = AccentGreen)
                }
            }
            TextButton(onClick = { onSetIgnored(tx.id, !tx.isIgnored) }) {
                Text(if (tx.isIgnored) "복원" else "무시", fontSize = 11.sp, color = TextSecondary)
            }
            TextButton(onClick = { onDelete(tx.id) }) {
                Text("삭제", fontSize = 11.sp, color = SpendRed)
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}

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

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

private fun formatTime(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDateTime().format(timeFormatter)
