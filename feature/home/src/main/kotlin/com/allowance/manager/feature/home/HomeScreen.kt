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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.allowance.manager.core.ui.theme.AmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

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
        onUpdateMemo = viewModel::onUpdateMemo,
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
    onUpdateMemo: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<Transaction?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(AmColors.ScreenBg)) {
            Hero(uiState = uiState, onNavigateToSetting = onNavigateToSetting)
            BottomContent(
                uiState = uiState,
                onToggleMainOnly = onToggleMainOnly,
                onSelect = { selected = it },
                modifier = Modifier.weight(1f),
            )
        }

        selected?.let { tx ->
            TransactionActionSheet(
                tx = tx,
                onDismiss = { selected = null },
                onSetIgnored = onSetIgnored,
                onDelete = onDelete,
                onPromoteToMain = onPromoteToMain,
                onUpdateMemo = onUpdateMemo,
            )
        }
    }
}

// ── 히어로 ───────────────────────────────────────────
@Composable
private fun Hero(uiState: HomeUiState, onNavigateToSetting: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmColors.Navy)
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
            .background(AmColors.HeroIconBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 13.sp)
    }
}

@Composable
private fun BudgetRing(remaining: Long, ratio: Float, isOver: Boolean, modifier: Modifier = Modifier) {
    val percent = ratio.coerceIn(0f, 1f)
    val ringColor = if (isOver) AmColors.Red else AmColors.Emerald
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
            drawArc(AmColors.HeroRingTrack, 0f, 360f, false, topLeft, arcSize, style = stroke)
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
                color = if (isOver) AmColors.Red else Color.White,
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AmColors.HeroPillBg),
    ) {
        PillItem("${uiState.spent.amountToComma()}원", "이번달 지출", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(44.dp).background(AmColors.HeroPillLine))
        PillItem("${uiState.budget.amountToComma()}원", "월 예산", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(44.dp).background(AmColors.HeroPillLine))
        PillItem(uiState.cycleLabel.substringAfter("다음 수급일 ").ifBlank { "-" }, "수급일까지", Modifier.weight(1f), AmColors.Emerald)
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
    onSelect: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(AmColors.ScreenBg),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item { BudgetProgressCard(uiState = uiState) }
            item { ListHeader(showMainOnly = uiState.showMainOnly, canFilter = uiState.canFilter, onToggleMainOnly = onToggleMainOnly) }

            if (uiState.transactions.isEmpty()) {
                item { EmptyState() }
            } else {
                items(uiState.transactions, key = { it.id }) { tx ->
                    TransactionCard(tx = tx, onClick = { onSelect(tx) })
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
    val barColor = if (uiState.isOver) AmColors.Red else AmColors.Emerald
    val badgeBg = if (uiState.isOver) AmColors.RedBg else AmColors.EmeraldBg

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AmColors.CardBg).padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("이번달 예산", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmColors.TextPrimary)
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
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(AmColors.ScreenBg),
        ) {
            Box(Modifier.fillMaxWidth(percent).height(7.dp).clip(RoundedCornerShape(4.dp)).background(barColor))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${uiState.spent.amountToComma()}원 사용", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = barColor)
            Text("${uiState.budget.amountToComma()}원 중", fontSize = 10.sp, color = AmColors.TextSecondary)
        }
    }
}

@Composable
private fun ListHeader(showMainOnly: Boolean, canFilter: Boolean, onToggleMainOnly: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("이번달 내역", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = AmColors.TextPrimary)
        if (canFilter) {
            Text(
                text = if (showMainOnly) "전체 보기" else "메인만 보기",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AmColors.Emerald,
                modifier = Modifier.clickable(onClick = onToggleMainOnly),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AmColors.CardBg).padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("이번달 내역이 없어요", fontSize = 11.sp, color = AmColors.TextSecondary)
    }
}

@Composable
private fun TransactionCard(tx: Transaction, onClick: () -> Unit) {
    val ignored = tx.isIgnored
    val dim = ignored || !tx.isMain
    val isIncome = tx.type == TransactionType.INCOME || tx.amount < 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            // 무시 항목은 카드 자체를 회색으로 → 합계에서 빠졌음을 확실히 표현
            .background(if (ignored) AmColors.IgnoredBg else AmColors.CardBg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (dim) AmColors.Divider else AmColors.EmeraldBg),
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
                        color = if (dim) AmColors.TextSecondary else AmColors.TextPrimary,
                        textDecoration = if (ignored) TextDecoration.LineThrough else TextDecoration.None,
                    )
                    if (ignored) {
                        Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.CardBg).padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text("숨김", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AmColors.TextSecondary)
                        }
                    } else if (!tx.isMain) {
                        Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.Divider).padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text("미등록", fontSize = 8.sp, color = AmColors.TextSecondary)
                        }
                    }
                }
                val subtitle = tx.memo?.takeIf { it.isNotBlank() } ?: formatTime(tx.createdAt)
                Text(subtitle, fontSize = 9.sp, color = AmColors.TextTertiary)
            }
        }
        Text(
            text = signedAmount(tx),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = amountColor(tx, dim),
            textDecoration = if (ignored) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionActionSheet(
    tx: Transaction,
    onDismiss: () -> Unit,
    onSetIgnored: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onPromoteToMain: (Transaction) -> Unit,
    onUpdateMemo: (Long, String) -> Unit,
) {
    var memo by remember(tx.id) { mutableStateOf(tx.memo.orEmpty()) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AmColors.CardBg) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tx.sourceName, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = AmColors.TextPrimary)
                    Text(formatTime(tx.createdAt), fontSize = 11.sp, color = AmColors.TextSecondary)
                }
                Text(signedAmount(tx), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = amountColor(tx, false))
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SheetAction("메모 저장", AmColors.Emerald) { onUpdateMemo(tx.id, memo); onDismiss() }

            if (!tx.isMain && tx.extractedAccount != null) {
                SheetAction("메인으로 등록", AmColors.Emerald) { onPromoteToMain(tx); onDismiss() }
            }
            SheetAction(if (tx.isIgnored) "숨김 해제" else "숨김 처리 (합계 제외)", AmColors.TextPrimary) {
                onSetIgnored(tx.id, !tx.isIgnored); onDismiss()
            }
            SheetAction("삭제", AmColors.Red) { onDelete(tx.id); onDismiss() }
        }
    }
}

@Composable
private fun SheetAction(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    )
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
    dim -> AmColors.TextSecondary
    tx.type == TransactionType.INCOME || tx.amount < 0 -> AmColors.Emerald
    else -> AmColors.Red
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

private fun formatTime(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDateTime().format(timeFormatter)
