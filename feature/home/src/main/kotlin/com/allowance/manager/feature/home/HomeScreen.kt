package com.allowance.manager.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.domain.model.LedgerFilter
import com.allowance.manager.core.domain.model.LedgerFilterChip
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmSecondaryButton
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.LocalAnalyticsHelper
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
import com.allowance.manager.core.ui.transaction.LedgerListHeader
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
    guideTargets: SnapshotStateMap<String, Rect>,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showGuide by viewModel.showGuide.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        guideTargets = guideTargets,
        onFilterChip = viewModel::onFilterChip,
        onSetHidden = viewModel::onSetHidden,
        onIgnoreSource = viewModel::onIgnoreSource,
        countIgnorable = viewModel::countIgnorable,
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
    // 바텀 네비(월별/통계)까지 가리키려면 좌표 저장소를 상위(AppNavHost)에서 공유받는다
    guideTargets: SnapshotStateMap<String, Rect> = rememberGuideTargets(),
    onFilterChip: (LedgerFilterChip) -> Unit = {},
    onSetHidden: (Long, Boolean) -> Unit = { _, _ -> },
    onIgnoreSource: (Transaction) -> Unit = {},
    countIgnorable: suspend (Transaction) -> Int = { 0 },
    onDelete: (Long) -> Unit = {},
    onPromoteToMain: (Transaction) -> Unit = {},
    onSaveTransaction: (Long, String, TransactionCategory?) -> Unit = { _, _, _ -> },
    onAddTransaction: (TransactionType, Long, String, TransactionCategory?, String) -> Unit = { _, _, _, _, _ -> },
    showGuide: Boolean = false,
    onGuideFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val analytics = LocalAnalyticsHelper.current
    var selected by remember { mutableStateOf<Transaction?>(null) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    // 화면 진입 직후 바로 띄우면 어색 → 0.5초 뒤에 가이드 노출 (이후 SpotlightGuide가 페이드 인)
    var guideReady by remember { mutableStateOf(false) }
    LaunchedEffect(showGuide) {
        guideReady = false
        if (showGuide) {
            delay(500)
            guideReady = true
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingToast by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    // 저장/추가 즉시 트리거 → 시트가 거의 내려간 ~0.2s 뒤 햅틱과 함께 노출 (기존 시트 닫힘 대기 대비 약 0.1s 빠름)
    LaunchedEffect(pendingToast) {
        val msg = pendingToast ?: return@LaunchedEffect
        delay(200)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        withTimeoutOrNull(1500) { snackbarHostState.showSnackbar(msg) }
        pendingToast = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(AmColors.ScreenBg)) {
            Hero(uiState = uiState, guideTargets = guideTargets)
            BottomContent(
                uiState = uiState,
                onFilterChip = onFilterChip,
                onSelect = { analytics.logEvent(AmAnalytics.Event.HOME_TX_ITEM_CLICK); selected = it },
                onToggleHidden = { onSetHidden(it.id, !it.isHidden) },
                onRequestDelete = { pendingDelete = it },
                guideTargets = guideTargets,
                // 예시(디폴트) 아이템은 가이드가 떠 있을 때만 노출 → 가이드 종료 시 사라짐
                showSample = showGuide,
                modifier = Modifier.weight(1f),
            )
        }

        selected?.let { tx ->
            TransactionDetailSheet(
                tx = tx,
                onDismiss = { selected = null },
                onSetHidden = onSetHidden,
                onDelete = onDelete,
                onPromoteToMain = onPromoteToMain,
                onSaveTransaction = onSaveTransaction,
                onSaved = { pendingToast = "저장이 완료되었습니다." },
                onIgnoreSource = onIgnoreSource,
                countIgnorable = countIgnorable,
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
            onClick = { analytics.logEvent(AmAnalytics.Event.HOME_ADD_FAB_CLICK); showAdd = true },
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
        if (guideReady && showGuide && selected == null && !showAdd) {
            SpotlightGuide(
                steps = homeGuideSteps(uiState.userType),
                targets = guideTargets,
                onFinish = onGuideFinished,
            )
        }
    }
}

private fun homeGuideSteps(type: UserType): List<GuideStep> = listOf(
    GuideStep(
        "hero",
        "이번 달 남은 ${type.label}${type.objectParticle} 한눈에 파악할 수 있어요.\n지출이 발생할 때마다 실시간으로 줄어들어요.",
        SpotShape.RECT,
    ),
    GuideStep(
        "expenseIncome",
        "이번 달 지출과 수입,\n${type.label} 관리를 돕는 정보를 확인해요.",
        SpotShape.RECT,
    ),
    GuideStep(
        "firstItem",
        "입출금 내역이 여기에 쌓여요.\n클릭해서 카테고리, 메모 등 자세한 정보를 기록할 수 있어요.",
        SpotShape.RECT,
    ),
    GuideStep(
        "fab",
        "자동으로 잡히지 않는 입·출금은\n직접 추가할 수 있어요.",
        SpotShape.CIRCLE,
    ),
    GuideStep(
        listOf("calendar", "stats"),
        "월별로 내역을 자세히 보고,\n통계에서 소비 추이와 카테고리별 분석을 확인해\n체계적으로 관리해요.",
        SpotShape.RECT,
    ),
)

// ── 히어로 ───────────────────────────────────────────
@Composable
private fun Hero(
    uiState: HomeUiState,
    guideTargets: SnapshotStateMap<String, Rect>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmColors.ScreenBg)
            .padding(horizontal = AmSpacing.xl)
            .padding(top = 12.dp, bottom = 4.dp),
    ) {
        Box(Modifier.fillMaxWidth().guideTarget("hero", guideTargets)) { BudgetCard(uiState = uiState) }
        Spacer(Modifier.height(12.dp))
        // 가이드 2번: 지출·수입 + 하루 지출 권장 지표까지 하나의 하이라이트로 묶음
        Box(Modifier.fillMaxWidth().guideTarget("expenseIncome", guideTargets)) {
            Column {
                ExpenseIncomeCard(uiState = uiState)
                Spacer(Modifier.height(12.dp))
                StatsRow(uiState = uiState)
            }
        }
    }
}

// 이번 달 남은 용돈 — 다크 카드 + 소진율 바 (바 아래 양 끝: 소진율·예산)
// 진입/클릭 시 금액은 0→현재치 카운트업, 소진율 바는 0→현재치 채움.
@Composable
private fun BudgetCard(uiState: HomeUiState) {
    val fillColor = if (uiState.isOver) AmColors.Red else AmColors.Emerald
    val targetRatio = if (uiState.isOver) 1f else uiState.spentRatio

    var playKey by remember { mutableIntStateOf(0) }
    val amountAnim = remember { Animatable(0f) }
    val ratioAnim = remember { Animatable(0f) }
    // 진입(최초 컴포지션)·클릭(playKey)·데이터 변경 시 0부터 재생
    LaunchedEffect(playKey, uiState.remaining, targetRatio) {
        amountAnim.snapTo(0f)
        ratioAnim.snapTo(0f)
        launch { amountAnim.animateTo(uiState.remaining.toFloat(), tween(durationMillis = 500)) }
        ratioAnim.animateTo(targetRatio, tween(durationMillis = 500))
    }
    val shownAmount = amountAnim.value.toLong()

    AmCard(
        modifier = Modifier.fillMaxWidth(),
        color = AmColors.HeroBg,
        shape = AmShape.cardLarge,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 30.dp),
        rippleColor = Color.White,          // 어두운 카드 → 흰색 리플
        onClick = { playKey++ },            // 탭하면 다시 재생
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "이번 달 남은 ${uiState.userType.label}",
                style = AmType.labelSoft.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (uiState.isOver) "-${abs(shownAmount).amountToComma()}원" else "${shownAmount.amountToComma()}원",
                style = AmType.amountHero,
                color = if (uiState.isOver) AmColors.Red else Color.White,
            )
            Spacer(Modifier.height(23.dp))
            // 소진율 바 (풀 너비) — 채움 비율 애니메이션
            AmProgressBar(
                ratio = ratioAnim.value,
                fillColor = fillColor,
                trackColor = AmColors.HeroBarTrack,
                height = 9.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            // 바 아래 양 끝: 좌 소진율 / 우 예산 (바를 두 값이 감싸도록)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // 퍼센트도 바와 함께 0부터 카운트업
                    text = if (uiState.isOver) "초과" else "${(ratioAnim.value * 100).toInt()}% 사용",
                    style = AmType.labelStrong,
                    color = fillColor,
                )
                Text(
                    "${uiState.userType.label} ${uiState.budget.amountToComma()}원",
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
            StatCell(dday, "${uiState.userType.paydayShort}까지", Modifier.weight(1f), AmColors.Emerald)
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
    onFilterChip: (LedgerFilterChip) -> Unit,
    onSelect: (Transaction) -> Unit,
    onToggleHidden: (Transaction) -> Unit,
    onRequestDelete: (Transaction) -> Unit,
    guideTargets: SnapshotStateMap<String, Rect>,
    showSample: Boolean,
    modifier: Modifier = Modifier,
) {
    // 월별과 동일 구조: 헤더는 clip(sheetTop) 밖, 리스트만 독립 clip Box 안.
    // → 리스트 애니메이션 잔상이 헤더(입출금 내역) 영역으로 새지 않도록 클리핑됨.
    Column(modifier = modifier) {
        LedgerListHeader(
            filter = uiState.filter,
            onFilterChip = onFilterChip,
            allBadge = uiState.showAllBadge,
            modifier = Modifier.padding(horizontal = AmSpacing.xl, vertical = AmSpacing.md),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(AmShape.sheetTop)
                .background(AmColors.ScreenBg),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = AmSpacing.xl, vertical = AmSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (uiState.transactions.isEmpty()) {
                    // 신규 유저(빈 리스트): 가이드 3번째 스텝 대상을 EmptyState로 잡아 안내
                    item {
                        Box(Modifier.fillMaxWidth().guideTarget("firstItem", guideTargets)) {
                            EmptyState(filter = uiState.filter, showSample = showSample)
                        }
                    }
                } else {
                    itemsIndexed(uiState.transactions, key = { _, tx -> tx.id }) { index, tx ->
                        // 왼쪽으로 밀면 숨김·삭제 액션 노출. 첫 항목은 가이드 대상으로 등록.
                        val rowModifier = if (index == 0) {
                            Modifier.animateItem().guideTarget("firstItem", guideTargets)
                        } else {
                            Modifier.animateItem()
                        }
                        SwipeRevealRow(
                            hidden = tx.isHidden,
                            onToggleHidden = { onToggleHidden(tx) },
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
}

@Composable
private fun EmptyState(filter: LedgerFilter, showSample: Boolean) {
    // 전체(=신규 유저, 데이터 자체가 없음)
    if (filter.isAll) {
        // 예시(디폴트) 아이템은 가이드가 떠 있을 때만. 가이드 종료 후엔 담백한 안내로.
        if (showSample) {
            SampleHint()
        } else {
            AmCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 28.dp)) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "아직 내역이 없어요.\n결제 알림이 오면 자동으로 기록돼요.",
                        style = AmType.caption,
                        color = AmColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
        return
    }
    // 필터로 인해 비어 있는 경우: 안내 문구만
    val message = when {
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

// 신규 유저용 예시 아이템 — 흐린 '예시' 배지 + 실제 내역 모양. 결제가 감지되면 이 자리에 실데이터가 쌓인다.
@Composable
private fun SampleHint() {
    Column(modifier = Modifier.fillMaxWidth()) {
        AmCard(
            modifier = Modifier.fillMaxWidth().alpha(0.6f),
            contentPadding = PaddingValues(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(AmColors.EmeraldBg),
                        contentAlignment = Alignment.Center,
                    ) { Text("☕", fontSize = 15.sp) }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("스타벅스", style = AmType.label, color = AmColors.TextPrimary)
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.Divider).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("예시", style = AmType.tag, color = AmColors.TextSecondary)
                            }
                        }
                        Text("오후 1:30", style = AmType.tiny, color = AmColors.TextTertiary)
                    }
                }
                Text("-5,600원", style = AmType.labelStrong, color = AmColors.Red)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "결제 알림이 오면 이렇게 자동으로 기록돼요.",
            style = AmType.caption,
            color = AmColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
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
    // 디폴트 카테고리 = 기타
    var category by remember { mutableStateOf<TransactionCategory?>(TransactionCategory.ETC) }
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
                AmSecondaryButton("취소", onClick = { close() }, modifier = Modifier.weight(1f))
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
