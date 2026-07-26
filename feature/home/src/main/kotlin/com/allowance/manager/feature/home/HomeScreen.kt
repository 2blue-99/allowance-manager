package com.allowance.manager.feature.home

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmProgressBar
import com.allowance.manager.core.designsystem.component.AmToggle
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
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
            .background(AmColors.ScreenBg)
            .padding(horizontal = AmSpacing.xl)
            .padding(top = 12.dp, bottom = 4.dp),
    ) {
        // 타이틀 행 — 라이트 배경 위 다크 텍스트
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("가계부", style = AmType.title, color = AmColors.TextPrimary)
            IconBtn(label = "⚙️", onClick = onNavigateToSetting)
        }

        Spacer(Modifier.height(6.dp))
        BudgetCard(uiState = uiState)
        Spacer(Modifier.height(12.dp))
        StatsRow(uiState = uiState)
    }
}

@Composable
private fun IconBtn(label: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(AmShape.pill)
            .background(AmColors.ChipBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 15.sp)
    }
}

// 이번 달 남은 용돈 — 다크 카드 + 소진율 바 (양 끝: 지출·예산)
@Composable
private fun BudgetCard(uiState: HomeUiState) {
    val fillColor = if (uiState.isOver) AmColors.Red else AmColors.Emerald
    AmCard(
        modifier = Modifier.fillMaxWidth(),
        color = AmColors.HeroBg,
        shape = AmShape.cardLarge,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "이번 달 남은 용돈",
                style = AmType.labelSoft,
                color = Color.White.copy(alpha = 0.45f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (uiState.isOver) "-${abs(uiState.remaining).amountToComma()}원" else "${uiState.remaining.amountToComma()}원",
                style = AmType.amountHero,
                color = if (uiState.isOver) AmColors.Red else Color.White,
            )
            Spacer(Modifier.height(22.dp))
            // 소진율 바 + "X% 사용"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AmProgressBar(
                    ratio = if (uiState.isOver) 1f else uiState.spentRatio,
                    fillColor = fillColor,
                    trackColor = AmColors.HeroBarTrack,
                    height = 8.dp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (uiState.isOver) "초과" else "${(uiState.spentRatio * 100).toInt()}% 사용",
                    style = AmType.labelStrong,
                    color = fillColor,
                )
            }
            Spacer(Modifier.height(9.dp))
            // 바 양 끝 정보: 지출 / 예산
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("지출 ${uiState.spent.amountToComma()}원", style = AmType.caption, color = Color.White.copy(alpha = 0.4f))
                Text("예산 ${uiState.budget.amountToComma()}원", style = AmType.caption, color = Color.White.copy(alpha = 0.4f))
            }
        }
    }
}

// 하루 권장 / 하루 평균 / 월급일까지 — 라이트 카드 3칸
@Composable
private fun StatsRow(uiState: HomeUiState) {
    val dday = if (uiState.daysUntilPayday <= 0) "오늘" else "D-${uiState.daysUntilPayday}"
    AmCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatCell("${uiState.dailyBudget.amountToComma()}원", "하루 권장", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(36.dp).background(AmColors.Divider))
            // 과속(평균>권장)이면 빨강으로 경고
            StatCell(
                "${uiState.dailyAverage.amountToComma()}원",
                "하루 평균",
                Modifier.weight(1f),
                if (uiState.overPace) AmColors.Red else AmColors.TextPrimary,
            )
            Box(Modifier.width(1.dp).height(36.dp).background(AmColors.Divider))
            StatCell(dday, "월급일까지", Modifier.weight(1f), AmColors.Emerald)
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = AmColors.TextPrimary) {
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = AmType.valueStrong, color = valueColor)
        Spacer(Modifier.height(3.dp))
        Text(label, style = AmType.micro, color = AmColors.TextSecondary)
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
            .clip(AmShape.sheetTop)
            .background(AmColors.ScreenBg),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = AmSpacing.xl, vertical = AmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
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
private fun ListHeader(showMainOnly: Boolean, canFilter: Boolean, onToggleMainOnly: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("이번달 내역", style = AmType.labelStrong, color = AmColors.TextPrimary)
        if (canFilter) {
            // 라벨은 고정하고 토글 on/off로 상태를 표현
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AmToggle(checked = showMainOnly, onCheckedChange = { onToggleMainOnly() })
                Text(
                    text = "메인만 보기",
                    style = AmType.captionStrong,
                    color = if (showMainOnly) AmColors.TextPrimary else AmColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    AmCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 28.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("이번달 내역이 없어요", style = AmType.caption, color = AmColors.TextSecondary)
        }
    }
}

@Composable
private fun TransactionCard(tx: Transaction, onClick: () -> Unit) {
    val ignored = tx.isIgnored
    val dim = ignored || !tx.isMain
    val isIncome = tx.type == TransactionType.INCOME || tx.amount < 0

    AmCard(
        modifier = Modifier.fillMaxWidth(),
        // 무시 항목은 카드 자체를 회색으로 → 합계에서 빠졌음을 확실히 표현
        color = if (ignored) AmColors.IgnoredBg else AmColors.CardBg,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (dim) AmColors.Divider else AmColors.EmeraldBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (isIncome) "💰" else "💳", fontSize = 15.sp)
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        tx.sourceName,
                        style = AmType.label,
                        color = if (dim) AmColors.TextSecondary else AmColors.TextPrimary,
                        textDecoration = if (ignored) TextDecoration.LineThrough else TextDecoration.None,
                    )
                    if (ignored) {
                        Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.CardBg).padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text("숨김", style = AmType.tag, color = AmColors.TextSecondary)
                        }
                    } else if (!tx.isMain) {
                        Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.Divider).padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text("미등록", style = AmType.tag, color = AmColors.TextSecondary)
                        }
                    }
                }
                val subtitle = tx.memo?.takeIf { it.isNotBlank() } ?: formatTime(tx.createdAt)
                Text(subtitle, style = AmType.tiny, color = AmColors.TextTertiary)
            }
        }
        Text(
            text = signedAmount(tx),
            style = AmType.labelStrong,
            color = amountColor(tx, dim),
            textDecoration = if (ignored) TextDecoration.LineThrough else TextDecoration.None,
        )
        }
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
                    Text(tx.sourceName, style = AmType.emphasis, color = AmColors.TextPrimary)
                    Text(formatTime(tx.createdAt), style = AmType.caption, color = AmColors.TextSecondary)
                }
                Text(signedAmount(tx), style = AmType.emphasis, color = amountColor(tx, false))
            }

            Spacer(Modifier.height(16.dp))
            AmTextField(
                value = memo,
                onValueChange = { memo = it },
                label = "메모",
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
        style = AmType.bodyStrong,
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
