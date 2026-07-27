package com.allowance.manager.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmOutlinedButton
import com.allowance.manager.core.designsystem.component.AmProgressBar
import com.allowance.manager.core.designsystem.component.AmToggle
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

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
        onSaveTransaction = viewModel::onSaveTransaction,
        onAddTransaction = viewModel::onAddTransaction,
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
    onSaveTransaction: (Long, String, TransactionCategory?) -> Unit = { _, _, _ -> },
    onAddTransaction: (TransactionType, Long, String, TransactionCategory?, String) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<Transaction?>(null) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingToast by remember { mutableStateOf<String?>(null) }
    // 시트가 완전히 내려간(=selected/showAdd 해제) 뒤 0.3초 있다가 스낵바 표시 (약 2초 유지)
    LaunchedEffect(selected, showAdd) {
        val msg = pendingToast
        if (selected == null && !showAdd && msg != null) {
            delay(300)
            withTimeoutOrNull(2000) { snackbarHostState.showSnackbar(msg) }
            pendingToast = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(AmColors.ScreenBg)) {
            Hero(uiState = uiState, onNavigateToSetting = onNavigateToSetting)
            BottomContent(
                uiState = uiState,
                onToggleMainOnly = onToggleMainOnly,
                onSelect = { selected = it },
                onIgnore = { onSetIgnored(it.id, !it.isIgnored) },
                onRequestDelete = { pendingDelete = it },
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
                onSaveTransaction = { id, m, c -> onSaveTransaction(id, m, c); pendingToast = "저장이 완료되었습니다." },
            )
        }

        // 스와이프 삭제도 확인을 거친다
        pendingDelete?.let { tx ->
            AmDialog(
                title = "내역을 삭제할까요?",
                onDismiss = { pendingDelete = null },
                onConfirm = { onDelete(tx.id); pendingDelete = null },
                confirmText = "삭제",
                confirmColor = AmColors.Red,
            ) {
                Text("삭제하면 되돌릴 수 없어요.", style = AmType.body, color = AmColors.TextSecondary)
            }
        }

        // 좌하단 추가 FAB
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = AmColors.Emerald,
            contentColor = Color.White,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "내역 추가")
        }

        if (showAdd) {
            AddTransactionSheet(
                onDismiss = { showAdd = false },
                onAdd = { t, a, s, c, m -> onAddTransaction(t, a, s, c, m); pendingToast = "내역이 추가되었습니다." },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )
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
    onIgnore: (Transaction) -> Unit,
    onRequestDelete: (Transaction) -> Unit,
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
            item { ListHeader(showMainOnly = uiState.showMainOnly, onToggleMainOnly = onToggleMainOnly) }

            if (uiState.transactions.isEmpty()) {
                item { EmptyState() }
            } else {
                items(uiState.transactions, key = { it.id }) { tx ->
                    // 왼쪽으로 밀면 무시·삭제 액션 노출
                    SwipeRevealRow(
                        ignored = tx.isIgnored,
                        onIgnore = { onIgnore(tx) },
                        onDelete = { onRequestDelete(tx) },
                        modifier = Modifier.animateItem(),
                    ) {
                        TransactionCard(tx = tx, onClick = { onSelect(tx) })
                    }
                }
            }
        }
    }
}

// 왼쪽 스와이프로 무시·삭제 액션을 드러내는 행 (draggable + Animatable, 안정 API)
@Composable
private fun SwipeRevealRow(
    ignored: Boolean,
    onIgnore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidth = 68.dp
    val maxReveal = with(density) { (actionWidth * 2).toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.clip(AmShape.card)) {
        // 배경: 오른쪽에 무시·삭제
        Row(modifier = Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
            SwipeAction(
                icon = if (ignored) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                label = if (ignored) "복원" else "무시",
                background = AmColors.ChipBg,
                foreground = AmColors.TextSecondary,
                width = actionWidth,
            ) { scope.launch { offsetX.animateTo(0f) }; onIgnore() }
            SwipeAction(
                icon = Icons.Outlined.Delete,
                label = "삭제",
                background = AmColors.Red,
                foreground = Color.White,
                width = actionWidth,
            ) { scope.launch { offsetX.animateTo(0f) }; onDelete() }
        }
        // 전경: 실제 카드 (드래그로 좌측 이동)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(-maxReveal, 0f)) }
                    },
                    onDragStopped = {
                        val target = if (offsetX.value < -maxReveal / 2f) -maxReveal else 0f
                        offsetX.animateTo(target)
                    },
                ),
        ) { content() }
    }
}

@Composable
private fun SwipeAction(
    icon: ImageVector,
    label: String,
    background: Color,
    foreground: Color,
    width: Dp,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = foreground, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = AmType.tiny, color = foreground)
    }
}

@Composable
private fun ListHeader(showMainOnly: Boolean, onToggleMainOnly: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("이번달 내역", style = AmType.labelStrong, color = AmColors.TextPrimary)
        // 메인만 보기 토글은 항상 노출
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
    // 수동 입력은 계좌가 없어도 정식 내역 → 미등록/흐림 대상 아님
    val dim = ignored || (!tx.isMain && !tx.isManual)
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
                Text(tx.category?.emoji ?: if (isIncome) "💰" else "💳", fontSize = 15.sp)
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
                    } else if (!tx.isMain && !tx.isManual) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TransactionActionSheet(
    tx: Transaction,
    onDismiss: () -> Unit,
    onSetIgnored: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onPromoteToMain: (Transaction) -> Unit,
    onSaveTransaction: (Long, String, TransactionCategory?) -> Unit,
) {
    // 항상 풀로 올라오게(부분 확장 금지)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // 편집 로컬 상태(취소 시 버려짐, 저장 시 upsert)
    var memo by remember(tx.id) { mutableStateOf(tx.memo.orEmpty()) }
    var category by remember(tx.id) { mutableStateOf(tx.category) }
    var ignored by remember(tx.id) { mutableStateOf(tx.isIgnored) }
    var promote by remember(tx.id) { mutableStateOf(tx.isMain) }
    var showDeleteConfirm by remember(tx.id) { mutableStateOf(false) }

    // 슬라이드 아웃 후 닫기
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AmColors.CardBg,
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.94f)) {
            // ── 스크롤 콘텐츠 (작은 화면 대응) ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                // 식별 헤더: 아이콘 + 금액(주인공) + 은행·시간 + 삭제(휴지통, 금액 행 우측 정렬)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(AmShape.card).background(AmColors.EmeraldBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isIncome = tx.type == TransactionType.INCOME || tx.amount < 0
                        Text(category?.emoji ?: if (isIncome) "💰" else "💳", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(signedAmount(tx), style = AmType.amountLarge, color = amountColor(tx, false))
                        Text("${tx.sourceName} · ${formatTime(tx.createdAt)}", style = AmType.caption, color = AmColors.TextSecondary)
                    }
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "삭제",
                        tint = AmColors.TextSecondary,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { showDeleteConfirm = true }
                            .padding(6.dp)
                            .size(22.dp),
                    )
                }

                Spacer(Modifier.height(20.dp))
                // 상단 관리 토글 묶음 (합계 제외 + 메인 계좌 등록)
                AmCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmColors.ScreenBg,
                    contentPadding = PaddingValues(horizontal = AmSpacing.lg, vertical = AmSpacing.xs),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SheetToggleRow(
                            title = "이번 달 합계에서 제외",
                            // 로컬 토글만 바꾸고 실제 반영은 저장 시
                            checked = ignored,
                            onCheckedChange = { ignored = it },
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .height(1.5.dp)
                                .background(AmColors.BarTrack),
                        )
                        SheetToggleRow(
                            title = "메인 계좌로 등록",
                            subtitle = tx.extractedAccount.orEmpty(),
                            // 로컬 토글만 바꾸고(취소 가능) 실제 승격은 저장 시 반영
                            checked = promote,
                            onCheckedChange = { promote = it },
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                SheetLabel("분류")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionCategory.entries.forEach { cat ->
                        AmChip(
                            label = "${cat.emoji} ${cat.label}",
                            selected = category == cat,
                        ) { category = if (category == cat) null else cat }
                    }
                }

                Spacer(Modifier.height(24.dp))
                SheetLabel("메모")
                Spacer(Modifier.height(10.dp))
                AmTextField(
                    value = memo,
                    // 줄바꿈 허용 + 최대 500자
                    onValueChange = { if (it.length <= MEMO_MAX) memo = it },
                    label = "메모를 남겨보세요",
                    singleLine = false,
                    minLines = 4,
                    supportingText = "${memo.length}/$MEMO_MAX",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))
            }

            // ── 하단 고정: 취소 / 저장 ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AmOutlinedButton("취소", onClick = { close() }, modifier = Modifier.weight(1f).height(52.dp))
                AmButton(
                    "저장",
                    onClick = {
                        // 저장 시 일괄 반영: 메모·분류 upsert + 합계 제외 + (ON이면) 메인 등록
                        onSaveTransaction(tx.id, memo, category)
                        if (ignored != tx.isIgnored) onSetIgnored(tx.id, ignored)
                        if (promote && !tx.isMain && tx.extractedAccount != null) onPromoteToMain(tx)
                        close()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                )
            }
        }
    }

    // 삭제 확인 (되돌릴 수 없는 동작 → 확인 후 삭제)
    if (showDeleteConfirm) {
        AmDialog(
            title = "내역을 삭제할까요?",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { showDeleteConfirm = false; onDelete(tx.id); close() },
            confirmText = "삭제",
            confirmColor = AmColors.Red,
        ) {
            Text("삭제하면 되돌릴 수 없어요.", style = AmType.body, color = AmColors.TextSecondary)
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = AmType.label, color = AmColors.TextSecondary)
}

@Composable
private fun SheetToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AmType.bodyStrong, color = AmColors.TextPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = AmType.caption, color = AmColors.TextSecondary)
            }
        }
        AmToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// 수동 내역 추가 시트 (좌하단 FAB에서 풀 모달로 오픈)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionSheet(
    onDismiss: () -> Unit,
    onAdd: (TransactionType, Long, String, TransactionCategory?, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<TransactionCategory?>(null) }
    var memo by remember { mutableStateOf("") }
    val amount = amountText.toLongOrNull() ?: 0L
    val canAdd = amount > 0 && source.isNotBlank()

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = AmColors.CardBg) {
        Column(modifier = Modifier.fillMaxHeight(0.94f)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Text("내역 추가", style = AmType.title, color = AmColors.TextPrimary)

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmChip("지출", type == TransactionType.EXPENSE) { type = TransactionType.EXPENSE }
                    AmChip("수입", type == TransactionType.INCOME) { type = TransactionType.INCOME }
                }

                Spacer(Modifier.height(20.dp))
                SheetLabel("금액")
                Spacer(Modifier.height(10.dp))
                AmTextField(
                    value = amountText,
                    onValueChange = { v -> amountText = v.filter { it.isDigit() } },
                    label = "금액 (원)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))
                SheetLabel("사용처")
                Spacer(Modifier.height(10.dp))
                AmTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = "예: 스타벅스",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))
                SheetLabel("분류")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionCategory.entries.forEach { cat ->
                        AmChip("${cat.emoji} ${cat.label}", category == cat) { category = if (category == cat) null else cat }
                    }
                }

                Spacer(Modifier.height(24.dp))
                SheetLabel("메모")
                Spacer(Modifier.height(10.dp))
                AmTextField(
                    value = memo,
                    onValueChange = { if (it.length <= MEMO_MAX) memo = it },
                    label = "메모를 남겨보세요",
                    singleLine = false,
                    minLines = 3,
                    supportingText = "${memo.length}/$MEMO_MAX",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AmOutlinedButton("취소", onClick = { close() }, modifier = Modifier.weight(1f).height(52.dp))
                AmButton(
                    "추가",
                    onClick = { onAdd(type, amount, source, category, memo); close() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = canAdd,
                )
            }
        }
    }
}

// ── helpers ──────────────────────────────────────────
private const val MEMO_MAX = 500

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
