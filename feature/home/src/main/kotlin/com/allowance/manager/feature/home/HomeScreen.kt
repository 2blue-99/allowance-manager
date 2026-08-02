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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
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
import com.allowance.manager.core.domain.model.HomeFilter
import com.allowance.manager.core.domain.model.HomeFilterChip
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
import com.allowance.manager.core.designsystem.component.AmThousandsTransformation
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.ui.guide.GuideStep
import com.allowance.manager.core.ui.guide.SpotShape
import com.allowance.manager.core.ui.guide.SpotlightGuide
import com.allowance.manager.core.ui.guide.guideTarget
import com.allowance.manager.core.ui.guide.rememberGuideTargets
import com.allowance.manager.core.ui.transaction.SwipeRevealRow
import com.allowance.manager.core.ui.transaction.TransactionDetailSheet
import com.allowance.manager.core.ui.transaction.TransactionRow
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
    val showGuide by viewModel.showGuide.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onNavigateToSetting = onNavigateToSetting,
        onFilterChip = viewModel::onFilterChip,
        onSetIgnored = viewModel::onSetIgnored,
        onDelete = viewModel::onDelete,
        onPromoteToMain = viewModel::onPromoteToMain,
        onSaveTransaction = viewModel::onSaveTransaction,
        onAddTransaction = viewModel::onAddTransaction,
        showGuide = showGuide,
        onGuideFinished = viewModel::onGuideFinished,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNavigateToSetting: () -> Unit = {},
    onFilterChip: (HomeFilterChip) -> Unit = {},
    onSetIgnored: (Long, Boolean) -> Unit = { _, _ -> },
    onDelete: (Long) -> Unit = {},
    onPromoteToMain: (Transaction) -> Unit = {},
    onSaveTransaction: (Long, String, TransactionCategory?) -> Unit = { _, _, _ -> },
    onAddTransaction: (TransactionType, Long, String, TransactionCategory?, String) -> Unit = { _, _, _, _, _ -> },
    showGuide: Boolean = false,
    onGuideFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<Transaction?>(null) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val guideTargets = rememberGuideTargets()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingToast by remember { mutableStateOf<String?>(null) }
    // 시트가 완전히 내려간(=selected/showAdd 해제) 뒤 0.3초 있다가 스낵바 표시 (약 2초 유지)
    LaunchedEffect(selected, showAdd) {
        val msg = pendingToast
        if (selected == null && !showAdd && msg != null) {
            withTimeoutOrNull(1500) { snackbarHostState.showSnackbar(msg) }
            pendingToast = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(AmColors.ScreenBg)) {
            Hero(uiState = uiState, onNavigateToSetting = onNavigateToSetting, guideTargets = guideTargets)
            BottomContent(
                uiState = uiState,
                onFilterChip = onFilterChip,
                onSelect = { selected = it },
                onIgnore = { onSetIgnored(it.id, !it.isIgnored) },
                onRequestDelete = { pendingDelete = it },
                guideTargets = guideTargets,
                modifier = Modifier.weight(1f),
            )
        }

        selected?.let { tx ->
            TransactionDetailSheet(
                tx = tx,
                onDismiss = { selected = null },
                onSetIgnored = onSetIgnored,
                onDelete = onDelete,
                onPromoteToMain = onPromoteToMain,
                onSaveTransaction = onSaveTransaction,
                onSaved = { pendingToast = "저장이 완료되었습니다." },
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
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).guideTarget("fab", guideTargets),
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

        // 최초 진입 가이드(스포트라이트). 시트가 안 떠 있을 때만.
        if (showGuide && selected == null && !showAdd) {
            SpotlightGuide(
                steps = homeGuideSteps(uiState.userType.label),
                targets = guideTargets,
                onFinish = onGuideFinished,
            )
        }
    }
}

private fun homeGuideSteps(label: String): List<GuideStep> = listOf(
    GuideStep("hero", "이번 달 쓸 수 있는 ${label}이에요.\n지출을 빼고 실시간으로 줄어들어요.", SpotShape.OVAL),
    GuideStep("expenseIncome", "이번 달 지출·수입을 한눈에.\n결제 알림을 자동 감지해 기록해줘요.", SpotShape.OVAL),
    GuideStep("firstItem", "내역을 탭하면 분류·메모를 남기고,\n메인으로 등록할 수 있어요.", SpotShape.OVAL),
    GuideStep("fab", "자동 감지 안 되는 현금 지출은\n여기서 직접 추가하세요.", SpotShape.CIRCLE),
    GuideStep("settings", "계좌 등록·${label} 변경 같은 건\n여기 설정에서 바꿀 수 있어요~", SpotShape.CIRCLE),
)

// ── 히어로 ───────────────────────────────────────────
@Composable
private fun Hero(
    uiState: HomeUiState,
    onNavigateToSetting: () -> Unit,
    guideTargets: SnapshotStateMap<String, Rect>,
) {
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
            Box(Modifier.guideTarget("settings", guideTargets)) {
                IconBtn(label = "⚙️", onClick = onNavigateToSetting)
            }
        }

        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().guideTarget("hero", guideTargets)) { BudgetCard(uiState = uiState) }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().guideTarget("expenseIncome", guideTargets)) { ExpenseIncomeCard(uiState = uiState) }
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

// 이번 달 남은 용돈 — 다크 카드 + 소진율 바 (바 아래 양 끝: 소진율·예산)
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
                "이번 달 남은 ${uiState.userType.label}",
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
            // 소진율 바 (풀 너비)
            AmProgressBar(
                ratio = if (uiState.isOver) 1f else uiState.spentRatio,
                fillColor = fillColor,
                trackColor = AmColors.HeroBarTrack,
                height = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(9.dp))
            // 바 아래 양 끝: 좌 소진율 / 우 예산 (바를 두 값이 감싸도록)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (uiState.isOver) "초과" else "${(uiState.spentRatio * 100).toInt()}% 사용",
                    style = AmType.labelStrong,
                    color = fillColor,
                )
                Text(
                    "예산 ${uiState.budget.amountToComma()}원",
                    style = AmType.caption,
                    color = Color.White.copy(alpha = 0.4f),
                )
            }
        }
    }
}

// 이번 달 지출 / 수입 — 라이트 카드 2칸 (가계부 실적)
@Composable
private fun ExpenseIncomeCard(uiState: HomeUiState) {
    AmCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SummaryCell("이번 달 지출", "${uiState.spent.amountToComma()}원", AmColors.Red, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(36.dp).background(AmColors.Divider))
            SummaryCell("이번 달 수입", "${uiState.income.amountToComma()}원", AmColors.Emerald, Modifier.weight(1f))
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

// 하루 권장 / 하루 평균 / 월급일까지 — 라이트 카드 3칸
@Composable
private fun StatsRow(uiState: HomeUiState) {
    val dday = if (uiState.daysUntilPayday <= 0) "오늘" else "D-${uiState.daysUntilPayday}"
    AmCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatCell("${uiState.dailyBudget.amountToComma()}원", "하루 지출 권장", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(36.dp).background(AmColors.Divider))
            // 과속(평균>권장)이면 빨강으로 경고
            StatCell(
                "${uiState.dailyAverage.amountToComma()}원",
                "하루 실지출",
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
    onFilterChip: (HomeFilterChip) -> Unit,
    onSelect: (Transaction) -> Unit,
    onIgnore: (Transaction) -> Unit,
    onRequestDelete: (Transaction) -> Unit,
    guideTargets: SnapshotStateMap<String, Rect>,
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
            item { ListHeader(filter = uiState.filter, onFilterChip = onFilterChip) }

            if (uiState.transactions.isEmpty()) {
                item { EmptyState(filter = uiState.filter) }
            } else {
                itemsIndexed(uiState.transactions, key = { _, tx -> tx.id }) { index, tx ->
                    // 왼쪽으로 밀면 무시·삭제 액션 노출. 첫 항목은 가이드 대상으로 등록.
                    val rowModifier = if (index == 0) {
                        Modifier.animateItem().guideTarget("firstItem", guideTargets)
                    } else {
                        Modifier.animateItem()
                    }
                    SwipeRevealRow(
                        ignored = tx.isIgnored,
                        onIgnore = { onIgnore(tx) },
                        onDelete = { onRequestDelete(tx) },
                        modifier = rowModifier,
                    ) {
                        TransactionRow(tx = tx, onClick = { onSelect(tx) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(filter: HomeFilter, onFilterChip: (HomeFilterChip) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("이번달 내역", style = AmType.labelStrong, color = AmColors.TextPrimary)
        // 메인 / 숨김 / 전체 — 메인·숨김은 독립 토글, 전체는 배타(둘 다 해제)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeFilterChip.entries.forEach { chip ->
                val selected = when (chip) {
                    HomeFilterChip.MAIN -> filter.showMain
                    HomeFilterChip.HIDDEN -> filter.showHidden
                    HomeFilterChip.ALL -> filter.isAll
                }
                AmChip(label = chip.label, selected = selected) { onFilterChip(chip) }
            }
        }
    }
}

@Composable
private fun EmptyState(filter: HomeFilter) {
    val message = when {
        filter.isAll -> "이번달 내역이 없어요"
        filter.showMain && filter.showHidden -> "표시할 내역이 없어요"
        filter.showMain -> "메인 내역이 없어요"
        else -> "숨긴 내역이 없어요"
    }
    AmCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 28.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(message, style = AmType.caption, color = AmColors.TextSecondary)
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = AmType.label, color = AmColors.TextSecondary)
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
                    // 저장값은 숫자만, 화면에는 천 단위 콤마로 표시
                    visualTransformation = AmThousandsTransformation(),
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
                    // supportingText = "${memo.length}/$MEMO_MAX",  // 글자수 표시는 생략
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AmOutlinedButton("취소", onClick = { close() }, modifier = Modifier.weight(1f))
                AmButton(
                    "추가",
                    onClick = { onAdd(type, amount, source, category, memo); close() },
                    modifier = Modifier.weight(1f),
                    enabled = canAdd,
                )
            }
        }
    }
}

// ── helpers ──────────────────────────────────────────
private const val MEMO_MAX = 500
