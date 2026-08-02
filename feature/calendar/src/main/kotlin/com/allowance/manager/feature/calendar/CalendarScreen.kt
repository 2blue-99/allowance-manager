package com.allowance.manager.feature.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.LedgerFilter
import com.allowance.manager.core.domain.model.LedgerFilterChip
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.ui.transaction.LedgerFilterChips
import com.allowance.manager.core.ui.transaction.SwipeRevealRow
import com.allowance.manager.core.ui.transaction.TransactionDetailSheet
import com.allowance.manager.core.ui.transaction.TransactionRow
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
        onToggleCategory = viewModel::onToggleCategory,
        onClearCategories = viewModel::onClearCategoryFilter,
        onFilterChip = viewModel::onFilterChip,
        onSetIgnored = viewModel::onSetIgnored,
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
    onToggleCategory: (TransactionCategory) -> Unit = {},
    onClearCategories: () -> Unit = {},
    onFilterChip: (LedgerFilterChip) -> Unit = {},
    onSetIgnored: (Long, Boolean) -> Unit = { _, _ -> },
    onDelete: (Long) -> Unit = {},
    onSaveTransaction: (Long, String, TransactionCategory?) -> Unit = { _, _, _ -> },
    onPromoteToMain: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<Transaction?>(null) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmColors.ScreenBg),
    ) {
        MonthNavBar(
            month = uiState.month,
            canGoPrev = uiState.canGoPrev,
            canGoNext = uiState.canGoNext,
            onPrev = onPrevMonth,
            onNext = onNextMonth,
            onPickMonth = { showMonthPicker = true },
        )

        Column(modifier = Modifier.padding(horizontal = AmSpacing.xl)) {
            SummaryCard(expense = uiState.expense, income = uiState.income)
            Spacer(Modifier.height(12.dp))
            ListHeader(
                filter = uiState.filter,
                searchActive = uiState.searchActive,
                onFilterChip = onFilterChip,
                onToggleSearch = onToggleSearch,
            )

            // 🔍 활성 시에만 검색창 + 분류 필터 노출 (해제하면 원복)
            AnimatedVisibility(visible = uiState.searchActive) {
                SearchAndFilter(
                    query = uiState.query,
                    categoryFilter = uiState.categoryFilter,
                    onQueryChange = onQueryChange,
                    onToggleCategory = onToggleCategory,
                    onClearCategories = onClearCategories,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        TransactionList(
            transactions = uiState.transactions,
            searchActive = uiState.searchActive,
            onSelect = { selected = it },
            onIgnore = { onSetIgnored(it.id, !it.isIgnored) },
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
        TransactionDetailSheet(
            tx = tx,
            onDismiss = { selected = null },
            onSetIgnored = onSetIgnored,
            onDelete = onDelete,
            onPromoteToMain = onPromoteToMain,
            onSaveTransaction = onSaveTransaction,
        )
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            current = uiState.month,
            minMonth = uiState.minMonth,
            maxMonth = YearMonth.now(),
            onSelect = { onSelectMonth(it); showMonthPicker = false },
            onDismiss = { showMonthPicker = false },
        )
    }
}

// ── 월 네비게이션 바 (‹ 2026년 7월 ▾ ›) ──
@Composable
private fun MonthNavBar(
    month: YearMonth,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPickMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AmSpacing.lg, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavArrow(icon = Icons.Filled.ChevronLeft, enabled = canGoPrev, contentDescription = "이전 달", onClick = onPrev)
        Row(
            modifier = Modifier
                .clip(AmShape.pill)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPickMonth,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${month.year}년 ${month.monthValue}월", style = AmType.header, color = AmColors.TextPrimary)
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "월 선택",
                tint = AmColors.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
        NavArrow(icon = Icons.Filled.ChevronRight, enabled = canGoNext, contentDescription = "다음 달", onClick = onNext)
    }
}

@Composable
private fun NavArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(AmShape.pill)
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) AmColors.TextPrimary else AmColors.BarTrack,
            modifier = Modifier.size(26.dp),
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
        Text(label, style = AmType.micro, color = AmColors.TextSecondary)
        Spacer(Modifier.height(5.dp))
        Text(value, style = AmType.valueStrong, color = valueColor)
    }
}

// ── 리스트 헤더 (제목 + 메인/숨김/전체 칩 + 🔍 토글) ──
@Composable
private fun ListHeader(
    filter: LedgerFilter,
    searchActive: Boolean,
    onFilterChip: (LedgerFilterChip) -> Unit,
    onToggleSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("이 달 내역", style = AmType.labelStrong, color = AmColors.TextPrimary)
        Spacer(Modifier.weight(1f))
        // 범위 필터(메인/숨김/전체)는 상시 노출 — 홈과 공용 칩
        LedgerFilterChips(filter = filter, onChip = onFilterChip)
        // 활성 시 초록 배경으로 눌린 상태 표현
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(AmShape.pill)
                .background(if (searchActive) AmColors.Emerald else AmColors.ChipBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleSearch,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "검색·필터",
                tint = if (searchActive) Color.White else AmColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── 검색창 + 분류 필터 (2줄 고정, 넘치면 가로 스크롤) ──
@Composable
private fun SearchAndFilter(
    query: String,
    categoryFilter: Set<TransactionCategory>,
    onQueryChange: (String) -> Unit,
    onToggleCategory: (TransactionCategory) -> Unit,
    onClearCategories: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        AmTextField(
            value = query,
            onValueChange = onQueryChange,
            label = "사용처·메모 검색",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        // 2줄 고정 그리드 — 열 우선으로 채우고, 넘치면 오른쪽으로 가로 스크롤
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().height(84.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // '전체' — 필터가 비어 있으면 기본 선택. 누르면 모든 필터 해제
            item(key = "ALL") {
                AmChip(label = "전체", selected = categoryFilter.isEmpty()) { onClearCategories() }
            }
            items(TransactionCategory.entries, key = { it.name }) { cat ->
                AmChip(
                    label = "${cat.emoji} ${cat.label}",
                    selected = cat in categoryFilter,
                ) { onToggleCategory(cat) }
            }
        }
    }
}

// ── 내역 리스트 ──
@Composable
private fun TransactionList(
    transactions: List<Transaction>,
    searchActive: Boolean,
    onSelect: (Transaction) -> Unit,
    onIgnore: (Transaction) -> Unit,
    onRequestDelete: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clip(AmShape.sheetTop).background(AmColors.ScreenBg)) {
        if (transactions.isEmpty()) {
            EmptyState(searchActive = searchActive)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = AmSpacing.xl, vertical = AmSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(transactions, key = { it.id }) { tx ->
                    SwipeRevealRow(
                        ignored = tx.isIgnored,
                        onIgnore = { onIgnore(tx) },
                        onDelete = { onRequestDelete(tx) },
                        modifier = Modifier.animateItem(),
                    ) {
                        TransactionRow(tx = tx, onClick = { onSelect(tx) })
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
                    if (searchActive) "조건에 맞는 내역이 없어요" else "이 달 내역이 없어요",
                    style = AmType.caption,
                    color = AmColors.TextSecondary,
                )
            }
        }
    }
}

// ── 월 선택 피커 (안드로이드 기본은 일 단위뿐 → 커스텀) ──
@Composable
private fun MonthPickerDialog(
    current: YearMonth,
    minMonth: YearMonth?,
    maxMonth: YearMonth,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var year by remember { mutableStateOf(current.year) }
    val minYear = minMonth?.year ?: (maxMonth.year - 10)
    val canPrevYear = year > minYear
    val canNextYear = year < maxMonth.year

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(AmShape.cardLarge)
                .background(AmColors.CardBg)
                .padding(20.dp),
        ) {
            // 연도 네비게이션
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavArrow(Icons.Filled.ChevronLeft, canPrevYear, "이전 해") { if (canPrevYear) year-- }
                Text("${year}년", style = AmType.title, color = AmColors.TextPrimary)
                NavArrow(Icons.Filled.ChevronRight, canNextYear, "다음 해") { if (canNextYear) year++ }
            }
            // 3열 x 4행 월 그리드
            (0 until 4).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    (0 until 3).forEach { col ->
                        val m = row * 3 + col + 1
                        val ym = YearMonth.of(year, m)
                        val enabled = ym >= (minMonth ?: ym) && ym <= maxMonth
                        MonthCell(
                            label = "${m}월",
                            selected = ym == current,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(ym) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCell(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = when {
        selected -> AmColors.Emerald
        else -> Color.Transparent
    }
    val fg = when {
        !enabled -> AmColors.BarTrack
        selected -> Color.White
        else -> AmColors.TextPrimary
    }
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(46.dp)
            .clip(AmShape.card)
            .background(bg)
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = AmType.bodyStrong, color = fg)
    }
}
