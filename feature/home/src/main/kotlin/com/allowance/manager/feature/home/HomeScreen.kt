package com.allowance.manager.feature.home

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.allowance.manager.core.domain.model.Announcement
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
import com.allowance.manager.core.ui.guide.GuideAction
import com.allowance.manager.core.ui.guide.GuideStep
import com.allowance.manager.core.ui.guide.SpotShape
import com.allowance.manager.core.ui.guide.SpotlightGuide
import com.allowance.manager.core.ui.guide.guideTarget
import com.allowance.manager.core.ui.guide.rememberGuideTargets
import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.ui.transaction.LedgerListHeader
import com.allowance.manager.core.ui.transaction.SwipeRevealRow
import com.allowance.manager.core.ui.transaction.TransactionFormSheet
import com.allowance.manager.core.ui.transaction.TransactionRow
import com.allowance.manager.core.ui.transaction.TxFormMode
import kotlinx.coroutines.delay
import java.time.LocalDate
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
    // 홈 가이드 위젯 스텝 배선 — feature 간 의존을 피해 app(위젯 모듈 접근)에서 주입한다.
    widgetPinSupported: Boolean = false,
    @DrawableRes widgetPreviewRes: Int = 0,
    onAddWidget: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showGuide by viewModel.showGuide.collectAsStateWithLifecycle()
    val announcement by viewModel.announcement.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        guideTargets = guideTargets,
        announcement = announcement,
        onAnnouncementDismissed = viewModel::onAnnouncementDismissed,
        onFilterChip = viewModel::onFilterChip,
        onSetHidden = viewModel::onSetHidden,
        onSetScope = viewModel::onSetScope,
        onIgnoreSource = viewModel::onIgnoreSource,
        countIgnorable = viewModel::countIgnorable,
        onDelete = viewModel::onDelete,
        onPromoteToMain = viewModel::onPromoteToMain,
        onSaveTransaction = viewModel::onSaveTransaction,
        onAddTransaction = viewModel::onAddTransaction,
        showGuide = showGuide,
        onGuideFinished = viewModel::onGuideFinished,
        widgetPinSupported = widgetPinSupported,
        widgetPreviewRes = widgetPreviewRes,
        onAddWidget = onAddWidget,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    // 바텀 네비(월별/통계)까지 가리키려면 좌표 저장소를 상위(AppNavHost)에서 공유받는다
    guideTargets: SnapshotStateMap<String, Rect> = rememberGuideTargets(),
    announcement: Announcement? = null,
    onAnnouncementDismissed: () -> Unit = {},
    onFilterChip: (LedgerFilterChip) -> Unit = {},
    onSetHidden: (Long, Boolean) -> Unit = { _, _ -> },
    onSetScope: (Long, TxScope) -> Unit = { _, _ -> },
    onIgnoreSource: (Transaction) -> Unit = {},
    countIgnorable: suspend (Transaction) -> Int = { 0 },
    onDelete: (Long) -> Unit = {},
    onPromoteToMain: (Transaction) -> Unit = {},
    onSaveTransaction: (Long, TransactionType, Long, String, String, TransactionCategory?) -> Unit = { _, _, _, _, _, _ -> },
    onAddTransaction: (TransactionType, Long, String, TransactionCategory?, String, TxScope) -> Unit = { _, _, _, _, _, _ -> },
    showGuide: Boolean = false,
    onGuideFinished: () -> Unit = {},
    widgetPinSupported: Boolean = false,
    @DrawableRes widgetPreviewRes: Int = 0,
    onAddWidget: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val analytics = LocalAnalyticsHelper.current
    var selected by remember { mutableStateOf<Transaction?>(null) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    // 직접추가 시 리스트를 맨 위로 올리는 트리거 (증가할 때마다 BottomContent가 top으로 스크롤)
    var addTick by remember { mutableIntStateOf(0) }
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
                addTick = addTick,
                modifier = Modifier.weight(1f),
            )
        }

        selected?.let { tx ->
            TransactionFormSheet(
                mode = TxFormMode.Edit(tx),
                onDismiss = { selected = null },
                budgetName = uiState.userType.label,
                onSetScope = onSetScope,
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
                Text("삭제하면 되돌릴 수 없어요.", style = AmType.size14_medium, color = AmColors.TextSecondary)
            }
        }

        // 홈 진입 공지(Remote Config) — 확인 버튼으로만 닫힘. 바깥탭/뒤로는 무동작(닫히지 않음).
        announcement?.let { noti ->
            AmDialog(
                title = noti.title,
                onDismiss = {},
                onConfirm = onAnnouncementDismissed,
                confirmText = "확인",
                dismissText = "",
                analyticsTag = AmAnalytics.Dialog.ANNOUNCEMENT,
                analyticsParams = mapOf(AmAnalytics.Param.ID to noti.id),
            ) {
                Text(noti.body, style = AmType.size14_medium, color = AmColors.TextSecondary)
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
            TransactionFormSheet(
                mode = TxFormMode.Add,
                onDismiss = { showAdd = false },
                onAdd = { t, a, s, c, m, sc -> onAddTransaction(t, a, s, c, m, sc); pendingToast = "내역이 추가되었습니다."; addTick++ },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )

        // 최초 진입 가이드(스포트라이트). 시트가 안 떠 있을 때만.
        if (guideReady && showGuide && selected == null && !showAdd) {
            SpotlightGuide(
                steps = homeGuideSteps(uiState.userType, widgetPinSupported, widgetPreviewRes, onAddWidget),
                targets = guideTargets,
                onFinish = onGuideFinished,
            )
        }
    }
}

// 홈 첫 진입 가이드: 헤드라인(큰 글씨) + 설명(작은 글씨) 2단. 마지막 스텝은 위젯 홍보(anchorless).
private fun homeGuideSteps(
    type: UserType,
    widgetPinSupported: Boolean,
    @DrawableRes widgetPreviewRes: Int,
    onAddWidget: () -> Unit,
): List<GuideStep> = listOf(
    GuideStep(
        "hero",
        "이번 달 ${type.label} 실시간 관리",
        "카드·계좌 알림을 자동으로 인식해서 이번 달 남은 ${type.label}${type.objectParticle} 실시간으로 관리해줘요.",
        SpotShape.RECT,
    ),
    GuideStep(
        "expenseIncome",
        "지출·수입을 한눈에",
        "이번 달 들어온 돈과 나간 돈을 한눈에 볼 수 있어요.",
        SpotShape.RECT,
    ),
    GuideStep(
        keys = listOf("firstItem"),
        title = "내역은 여기에 차곡차곡",
        message = "카드·계좌 알림을 인식해 내역이 자동으로 쌓여요. 그중 하나를 눌러 메인으로 등록하면 그 출처 알림이 모두 이번 달 ${type.label}에 반영돼요.",
        shape = SpotShape.RECT,
        emphasis = "그중 하나를 눌러 메인으로 등록하면 그 출처 알림이 모두 이번 달 ${type.label}에 반영",
    ),
    GuideStep(
        keys = listOf("filterChips"),
        title = "관리는 메인에서",
        message = "메인 필터를 누르면 관리하고 싶은 내역만 한눈에 볼 수 있어요. 전체에서 필요한 출처를 메인으로 등록한 뒤, 메인에서 관리하는 것을 추천해요.",
        shape = SpotShape.RECT,
        emphasis = "전체에서 필요한 출처를 메인으로 등록",
    ),
    GuideStep(
        "fab",
        "빠진 내역은 직접 추가",
        "자동으로 안 잡히는 입·출금은 직접 넣을 수 있어요.",
        SpotShape.CIRCLE,
    ),
    // 위젯 홍보 — 구멍 없이 전체 딤 + 위젯 미리보기. 런처가 고정 요청을 지원하면 "위젯 추가하기", 아니면 안내+"확인".
    GuideStep(
        keys = emptyList(),
        title = "위젯으로 한눈에",
        message = if (widgetPinSupported) {
            "위젯으로 남은 ${type.label}${type.objectParticle} 빠르게 확인하세요. 과소비를 막는 데 효과적이에요."
        } else {
            "홈 화면을 길게 눌러 '내돈지켜' 위젯을 추가할 수 있어요. 앱을 열지 않아도 남은 ${type.label}${type.objectParticle} 바로 확인해요."
        },
        image = widgetPreviewRes.takeIf { it != 0 },
        action = if (widgetPinSupported) {
            GuideAction("위젯 추가하기", onClick = onAddWidget, secondaryLabel = "다음에 할게요")
        } else {
            GuideAction("확인", onClick = {})
        },
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
        // 가이드 2번: 하루 지표는 예산 카드로 옮겨져 지출·수입 카드만 하이라이트한다.
        Box(Modifier.fillMaxWidth().guideTarget("expenseIncome", guideTargets)) {
            ExpenseIncomeCard(uiState = uiState)
        }
    }
}

// 예산 카드 머리글(라벨·기간) 투명도 — 주인공인 금액과 대비되게 낮춘 값
private const val HERO_CAPTION_ALPHA = 0.45f

/** 사이클 표기용 날짜 — "7.25" (연도 생략) */
private fun LocalDate.toCycleLabel(): String = "$monthValue.$dayOfMonth"

// 이번 달 남은 용돈 — 다크 카드 + 소진율 바 (바 아래 양 끝: 소진율·예산)
// 진입/클릭 시 금액은 0→현재치 카운트업, 소진율 바는 0→현재치 채움.
@Composable
private fun BudgetCard(uiState: HomeUiState) {
    val analytics = LocalAnalyticsHelper.current
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
        onClick = { analytics.logEvent(AmAnalytics.Event.HOME_BUDGET_CARD_CLICK); playKey++ },  // 탭하면 다시 재생
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 라벨 | 사이클 기간 — 카드의 주인공은 아래 금액이라 둘 다 같은 톤으로 물러나게 둔다.
            // 기간을 함께 적어 '이번 달'이 수급일 사이클임을 밝힌다. (월별 화면의 '달력 월'과 구분)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "남은 ${uiState.userType.label}",
                    style = AmType.size12_medium,
                    color = Color.White.copy(alpha = HERO_CAPTION_ALPHA),
                )
                if (uiState.cycleStart != null && uiState.cycleEnd != null) {
                    HeroCaptionDivider()
                    Text(
                        "${uiState.cycleStart.toCycleLabel()} – ${uiState.cycleEnd.toCycleLabel()}",
                        style = AmType.size12_medium,
                        color = Color.White.copy(alpha = HERO_CAPTION_ALPHA),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (uiState.isOver) "-${abs(shownAmount).amountToComma()}원" else "${shownAmount.amountToComma()}원",
                style = AmType.size36_black,
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
                    style = AmType.size12_medium,
                    color = fillColor,
                )
                Text(
                    "${uiState.budget.amountToComma()}원",
                    style = AmType.size12_medium,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
            // 하루 권장 | 하루 실지출 | D-day — 전부 예산 사이클에서 나오는 값이라 카드 안으로 들인다.
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f)),
            )
            Spacer(Modifier.height(15.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HeroStat("하루 지출 권장", "${uiState.dailyBudget.amountToComma()}원", Modifier.weight(1f))
                HeroStat(
                    "하루 실지출",
                    "${uiState.dailyAverage.amountToComma()}원",
                    Modifier.weight(1f),
                    // 과속(평균>권장)이면 빨강으로 경고
                    if (uiState.overPace) AmColors.Red else Color.White,
                )
                HeroStat(
                    "${uiState.userType.paydayShort}까지",
                    if (uiState.daysUntilPayday <= 0) "오늘" else "D-${uiState.daysUntilPayday}",
                    Modifier.weight(1f),
                    AmColors.Emerald,
                )
            }
        }
    }
}

/** 예산 카드 안 한 줄 항목을 잇는 세로 구분선 (머리글·지표 공용) */
@Composable
private fun HeroCaptionDivider() {
    Spacer(Modifier.width(8.dp))
    Box(
        Modifier
            .width(1.dp)
            .height(11.dp)
            .background(Color.White.copy(alpha = 0.22f)),
    )
    Spacer(Modifier.width(8.dp))
}

/** 예산 카드 아래 지표 한 칸 — 라벨은 머리글과 같은 톤으로 물러나고 그 아래 값이 선다. */
@Composable
private fun HeroStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = AmType.size11_medium, color = Color.White.copy(alpha = HERO_CAPTION_ALPHA))
        Spacer(Modifier.height(5.dp))
        Text(value, style = AmType.size14_bold, color = valueColor)
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
        Text(label, style = AmType.size10_medium, color = AmColors.TextSecondary)
        Spacer(Modifier.height(5.dp))
        Text(value, style = AmType.size16_bold, color = valueColor)
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
    addTick: Int,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // 화면 진입(초기 실행) + 직접추가(addTick 증가) 시에만 리스트를 맨 위로.
    // 배경 자동기록·수정·삭제로는 스크롤하지 않는다(편집 중 방해 방지).
    LaunchedEffect(addTick) { listState.animateScrollToItem(0) }

    // 월별과 동일 구조: 헤더는 clip(sheetTop) 밖, 리스트만 독립 clip Box 안.
    // → 리스트 애니메이션 잔상이 헤더(입출금 내역) 영역으로 새지 않도록 클리핑됨.
    Column(modifier = modifier) {
        LedgerListHeader(
            filter = uiState.filter,
            onFilterChip = onFilterChip,
            allBadge = uiState.showAllBadge,
            modifier = Modifier
                .padding(horizontal = AmSpacing.xl, vertical = AmSpacing.md)
                .guideTarget("filterChips", guideTargets),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(AmShape.sheetTop)
                .background(AmColors.ScreenBg),
        ) {
            LazyColumn(
                state = listState,
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
                        ) { cardShape ->
                            TransactionRow(tx = tx, budgetName = uiState.userType.label, shape = cardShape, onClick = { onSelect(tx) })
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
                        style = AmType.size11_medium,
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
    val message = if (filter.showMain) "메인 내역이 없어요" else "표시할 내역이 없어요"
    AmCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 28.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(message, style = AmType.size11_medium, color = AmColors.TextSecondary)
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
                            Text("스타벅스", style = AmType.size12_bold, color = AmColors.TextPrimary)
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.Divider).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("예시", style = AmType.size8_bold, color = AmColors.TextSecondary)
                            }
                        }
                        Text("오후 1:30", style = AmType.size9_medium, color = AmColors.TextTertiary)
                    }
                }
                Text("-5,600원", style = AmType.size12_black, color = AmColors.Red)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "결제 알림이 오면 이렇게 자동으로 기록돼요.",
            style = AmType.size11_medium,
            color = AmColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

