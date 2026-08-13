package com.allowance.manager.feature.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmSegmented
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.ui.month.MonthNavBar
import com.allowance.manager.core.ui.month.MonthPickerDialog
import com.allowance.manager.core.ui.transaction.categoryChipColor
import com.allowance.manager.core.ui.transaction.LedgerListHeader
import com.allowance.manager.core.ui.transaction.SwipeRevealRow
import com.allowance.manager.core.ui.transaction.TransactionFormSheet
import com.allowance.manager.core.ui.transaction.TransactionRow
import com.allowance.manager.core.ui.transaction.TxFormMode
import java.time.YearMonth

@Composable
fun CalendarRoute(
    initialMonth: YearMonth?,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    // 통계에서 딥링크로 특정 달을 열면 그 달로 이동 (최초 1회)
    LaunchedEffect(Unit) {
        if (initialMonth != null) viewModel.onSelectMonth(initialMonth)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        uiState = uiState,
        onPrevMonth = viewModel::onPrevMonth,
        onNextMonth = viewModel::onNextMonth,
        onSelectMonth = viewModel::onSelectMonth,
        onToggleSearch = viewModel::onToggleSearch,
        onQueryChange = viewModel::onQueryChange,
        onSetTypeFilter = viewModel::onSetTypeFilter,
        onToggleCategory = viewModel::onToggleCategory,
        onClearCategories = viewModel::onClearCategoryFilter,
        onSetHidden = viewModel::onSetHidden,
        onSetScope = viewModel::onSetScope,
        onIgnoreSource = viewModel::onIgnoreSource,
        countIgnorable = viewModel::countIgnorable,
        onDelete = viewModel::onDelete,
        onSaveTransaction = viewModel::onSaveTransaction,
        onPromoteToMain = viewModel::onPromoteToMain,
    )
}

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPrevMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onSelectMonth: (YearMonth) -> Unit = {},
    onToggleSearch: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onSetTypeFilter: (LedgerTypeFilter) -> Unit = {},
    onToggleCategory: (TransactionCategory) -> Unit = {},
    onClearCategories: () -> Unit = {},
    onSetHidden: (Long, Boolean) -> Unit = { _, _ -> },
    onSetScope: (Long, TxScope) -> Unit = { _, _ -> },
    onIgnoreSource: (Transaction) -> Unit = {},
    countIgnorable: suspend (Transaction) -> Int = { 0 },
    onDelete: (Long) -> Unit = {},
    onSaveTransaction: (Long, TransactionType, Long, String, String, TransactionCategory?) -> Unit = { _, _, _, _, _, _ -> },
    onPromoteToMain: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<Transaction?>(null) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmColors.ScreenBg)
            // 빈 곳(입력칸 외)을 탭하면 포커스 해제 → 키보드 내림
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
    ) {
        MonthNavBar(
            month = uiState.month,
            canGoPrev = uiState.canGoPrev,
            canGoNext = uiState.canGoNext,
            onPrev = onPrevMonth,
            onNext = onNextMonth,
            onPickMonth = { showMonthPicker = true },
            modifier = Modifier.padding(horizontal = AmSpacing.lg, vertical = 10.dp),
            prevDesc = "이전 달",
            nextDesc = "다음 달",
        )

        Column(modifier = Modifier.padding(horizontal = AmSpacing.xl)) {
            SummaryCard(expense = uiState.expense, income = uiState.income)
            Spacer(Modifier.height(12.dp))
            LedgerListHeader(
                showChips = false,   // 월별은 전체 고정 → 필터 칩 없이 제목 + 검색만
                trailing = { SearchToggle(active = uiState.searchActive, onToggle = onToggleSearch) },
            )

            // 🔍 활성 시에만 검색창 + 유형/분류 필터 노출 (해제하면 원복)
            AnimatedVisibility(visible = uiState.searchActive) {
                SearchAndFilter(
                    query = uiState.query,
                    typeFilter = uiState.typeFilter,
                    categoryFilter = uiState.categoryFilter,
                    onQueryChange = onQueryChange,
                    onSetTypeFilter = onSetTypeFilter,
                    onToggleCategory = onToggleCategory,
                    onClearCategories = onClearCategories,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        TransactionList(
            transactions = uiState.transactions,
            searchActive = uiState.searchActive,
            budgetName = uiState.userType.label,
            onSelect = { selected = it },
            onToggleHidden = { onSetHidden(it.id, !it.isHidden) },
            onRequestDelete = { pendingDelete = it },
            modifier = Modifier.weight(1f),
        )
    }

    // 스와이프 삭제도 확인을 거친다 (되돌릴 수 없는 동작)
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

    selected?.let { tx ->
        TransactionFormSheet(
            mode = TxFormMode.Edit(tx),
            onDismiss = { selected = null },
            budgetName = uiState.userType.label,
            onSetScope = onSetScope,
            onDelete = onDelete,
            onPromoteToMain = onPromoteToMain,
            onIgnoreSource = onIgnoreSource,
            countIgnorable = countIgnorable,
            onSaveTransaction = onSaveTransaction,
        )
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            current = uiState.month,
            minMonth = uiState.minMonth,
            maxMonth = YearMonth.now(),
            dataMonths = uiState.dataMonths,
            onSelect = { onSelectMonth(it); showMonthPicker = false },
            onDismiss = { showMonthPicker = false },
        )
    }
}

// ── 지출/수입 요약 (노출 리스트 기준) ──
@Composable
private fun SummaryCard(expense: Long, income: Long) {
    AmCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 18.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SummaryCell("지출", "${expense.amountToComma()}원", AmColors.Red, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(38.dp).background(AmColors.Divider))
            SummaryCell("수입", "${income.amountToComma()}원", AmColors.Emerald, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = AmType.labelMuted, color = AmColors.TextSecondary)
        Spacer(Modifier.height(5.dp))
        Text(value, style = AmType.bodyStrong, color = valueColor)
    }
}

// ── 검색·필터 토글 (공용 헤더의 우측 슬롯) ──
@Composable
private fun SearchToggle(active: Boolean, onToggle: () -> Unit) {
    // 활성 시 초록 배경으로 눌린 상태 표현
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(AmShape.pill)
            .background(if (active) AmColors.Emerald else AmColors.ChipBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = "검색·필터",
            tint = if (active) Color.White else AmColors.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ── 검색창 + 유형 슬라이드 + 분류 필터 (흰 카드 안에 정돈) ──
@Composable
private fun SearchAndFilter(
    query: String,
    typeFilter: LedgerTypeFilter,
    categoryFilter: Set<TransactionCategory>,
    onQueryChange: (String) -> Unit,
    onSetTypeFilter: (LedgerTypeFilter) -> Unit,
    onToggleCategory: (TransactionCategory) -> Unit,
    onClearCategories: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var searchFocused by remember { mutableStateOf(false) }
    // 검색 입력 중 뒤로가기는 화면 이탈 대신 포커스 해제(키보드 내림) 먼저
    BackHandler(enabled = searchFocused) { focusManager.clearFocus() }

    AmCard(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        contentPadding = PaddingValues(AmSpacing.lg),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AmTextField(
                value = query,
                onValueChange = onQueryChange,
                label = "출처·사용처·메모 검색",
                dense = true,
                modifier = Modifier.fillMaxWidth().onFocusChanged { searchFocused = it.isFocused },
            )
            Spacer(Modifier.height(12.dp))
            // 유형 슬라이드 (전체 / 지출 / 수입)
            AmSegmented(
                options = listOf("전체", "지출", "수입"),
                selectedIndex = when (typeFilter) {
                    LedgerTypeFilter.ALL -> 0
                    LedgerTypeFilter.EXPENSE -> 1
                    LedgerTypeFilter.INCOME -> 2
                },
                onSelect = {
                    onSetTypeFilter(
                        when (it) {
                            1 -> LedgerTypeFilter.EXPENSE
                            2 -> LedgerTypeFilter.INCOME
                            else -> LedgerTypeFilter.ALL
                        },
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
            // 분류 칩 — 2줄 고정, 넘치면 오른쪽으로 가로 스크롤(두 줄이 함께 스크롤).
            // 칩은 각자 폭만 차지 → 빈 공백 없음. 선택 시 카테고리색으로 채움. ALL = 전체 해제
            val total = 1 + TransactionCategory.entries.size
            @Composable
            fun filterCell(i: Int) {
                if (i == 0) {
                    AmChip(label = "ALL", selected = categoryFilter.isEmpty()) { onClearCategories() }
                } else {
                    val cat = TransactionCategory.entries[i - 1]
                    AmChip(
                        label = "${cat.emoji} ${cat.label}",
                        selected = cat in categoryFilter,
                        selectedColor = categoryChipColor(cat),
                    ) { onToggleCategory(cat) }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                // 열 우선 분배(위→아래→오른쪽): 원래 그리드 읽기 순서 유지
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    (0 until total step 2).forEach { filterCell(it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    (1 until total step 2).forEach { filterCell(it) }
                }
            }
        }
    }
}

// ── 내역 리스트 ──
@Composable
private fun TransactionList(
    transactions: List<Transaction>,
    searchActive: Boolean,
    budgetName: String = "예산",
    onSelect: (Transaction) -> Unit,
    onToggleHidden: (Transaction) -> Unit,
    onRequestDelete: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // 화면 진입 시 리스트를 맨 위로. (월별은 직접추가가 없어 진입 시에만)
    LaunchedEffect(Unit) { listState.scrollToItem(0) }
    Box(modifier = modifier.clip(AmShape.sheetTop).background(AmColors.ScreenBg)) {
        if (transactions.isEmpty()) {
            EmptyState(searchActive = searchActive)
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = AmSpacing.xl, vertical = AmSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(transactions, key = { it.id }) { tx ->
                    SwipeRevealRow(
                        hidden = tx.isHidden,
                        onToggleHidden = { onToggleHidden(tx) },
                        onDelete = { onRequestDelete(tx) },
                        modifier = Modifier.animateItem(),
                    ) {
                        TransactionRow(tx = tx, budgetName = budgetName, onClick = { onSelect(tx) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(searchActive: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = AmSpacing.xl, vertical = 40.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AmCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 32.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchActive) "조건에 맞는 내역이 없어요" else "입출금 내역이 없어요",
                    style = AmType.caption,
                    color = AmColors.TextSecondary,
                )
            }
        }
    }
}

