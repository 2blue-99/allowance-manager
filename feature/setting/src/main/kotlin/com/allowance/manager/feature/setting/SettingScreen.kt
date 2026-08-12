package com.allowance.manager.feature.setting

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
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
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmChevron
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.LocalAnalyticsHelper
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmScreenHeader
import com.allowance.manager.core.designsystem.component.AmSelectableOptionCard
import com.allowance.manager.core.designsystem.component.AmSettingGroup
import com.allowance.manager.core.designsystem.component.AmSettingItem
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.component.AmThousandsTransformation
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.ui.VerticalSpacer

private const val PAYDAY_EOM = 0

private fun paydayLabel(payday: Int): String = if (payday <= 0) "말일" else "${payday}일"

/** 상태 계층: ViewModel·네비게이션 의존성을 여기서 관리한다. */
@Composable
fun SettingRoute(
    onBack: () -> Unit,
    onNavigateToAccount: () -> Unit = {},
    onNavigateToIgnored: () -> Unit = {},
    // debug 빌드에서만 non-null → 디버그 진입 노출
    onNavigateToDebug: (() -> Unit)? = null,
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val analytics = LocalAnalyticsHelper.current
    // 설치된 앱의 versionName을 그대로 표시 (모듈 BuildConfig 결합 없이)
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }

    // 후원(인앱 결제) — 화면 생존 동안 클라이언트 유지, 이탈 시 연결 해제
    val donationManager = remember { DonationManager(context) }
    DisposableEffect(Unit) { onDispose { donationManager.release() } }
    var showThanks by remember { mutableStateOf(false) }

    SettingScreen(
        uiState = uiState,
        versionName = versionName,
        onBack = onBack,
        onNavigateToAccount = onNavigateToAccount,
        onNavigateToIgnored = onNavigateToIgnored,
        onNavigateToDebug = onNavigateToDebug,
        onStatusBarEnabledChange = viewModel::setStatusBarEnabled,
        onBudgetChange = viewModel::setBudget,
        onPaydayChange = viewModel::setPayday,
        onUserTypeChange = viewModel::setUserType,
        onSupport = {
            // debug 빌드(onNavigateToDebug != null)에선 미등록 상품이라 결제창이 안 뜸 → 바로 감사 다이얼로그로 결과 확인
            if (onNavigateToDebug != null) {
                showThanks = true
            } else {
                (context as? Activity)?.let { activity ->
                    donationManager.donate(activity) { result ->
                        analytics.logEvent(AmAnalytics.Event.DONATE_RESULT, mapOf(AmAnalytics.Param.RESULT to result.name.lowercase()))
                        if (result == DonationManager.Result.SUCCESS) showThanks = true
                    }
                }
            }
        },
        onFeedback = { context.startFeedbackEmail() },
        onRate = { context.openPlayStore() },
    )

    if (showThanks) {
        AmDialog(
            title = "감사합니다 ☕",
            onDismiss = { showThanks = false },
            onConfirm = { showThanks = false },
            confirmText = "닫기",
            dismissText = "",
            analyticsTag = AmAnalytics.Dialog.DONATE_THANKS,
        ) {
            Text(
                "이 한 잔이 저에게는 큰 힘이 됩니다.\n더 쓸만한 가계부로 보답하겠습니다!",
                fontSize = 14.sp,
                color = AmColors.TextSecondary,
            )
        }
    }
}

// devcoderblue@gmail.com 로 건의 메일 작성
private fun android.content.Context.startFeedbackEmail() {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:devcoderblue@gmail.com")
        putExtra(Intent.EXTRA_SUBJECT, "[가계부] 건의하기")
    }
    runCatching { startActivity(intent) }
}

// 플레이스토어 앱 페이지 (없으면 웹으로 폴백)
private fun android.content.Context.openPlayStore() {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    }.onFailure {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
    }
}

/** 표현 계층: uiState와 콜백만 받는다. (Preview 가능) */
@Composable
fun SettingScreen(
    uiState: SettingUiState,
    versionName: String = "",
    onBack: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToIgnored: () -> Unit = {},
    onNavigateToDebug: (() -> Unit)? = null,
    onStatusBarEnabledChange: (Boolean) -> Unit = {},
    onBudgetChange: (Long) -> Unit = {},
    onPaydayChange: (Int) -> Unit = {},
    onUserTypeChange: (UserType) -> Unit = {},
    onSupport: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onRate: () -> Unit = {},
) {
    val analytics = LocalAnalyticsHelper.current
    // 다이얼로그 노출 여부는 순수 UI 상태 → 화면이 직접 보유
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showPaydayDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(AmColors.ScreenBg).padding(top = AmSpacing.xl).padding(horizontal = AmSpacing.xl),
    ) {
        AmScreenHeader(title = "더보기", onBack = onBack)
        Spacer(Modifier.height(AmSpacing.xl))

        LazyColumn {
            item {
                // 후원 — 카테고리 없이 맨 위, 강조 카드
                SupportCard(onClick = { analytics.logEvent(AmAnalytics.Event.SETTING_DONATE_CLICK); onSupport() })
            }

            item {
                AmSettingGroup(
                    label = "${uiState.userType.label} 설정",
                    items = listOf(
                        {
                            AmSettingItem(title = "유형", subtitle = "이 금액을 부르는 호칭 (용돈/생활비/예산)", onClick = { showTypeDialog = true }) {
                                Text(uiState.userType.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmColors.Emerald)
                                Spacer(Modifier.width(AmSpacing.xs))
                                AmChevron()
                            }
                        },
                        {
                            AmSettingItem(title = "월 ${uiState.userType.label}", onClick = { showBudgetDialog = true }) {
                                Text("${uiState.budget.amountToComma()}원", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmColors.Emerald)
                                Spacer(Modifier.width(AmSpacing.xs))
                                AmChevron()
                            }
                        },
                        {
                            AmSettingItem(title = uiState.userType.paydayLabel, onClick = { showPaydayDialog = true }) {
                                Text(paydayLabel(uiState.payday), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmColors.Emerald)
                                Spacer(Modifier.width(AmSpacing.xs))
                                AmChevron()
                            }
                        },
                    ),
                )
            }

            item {
                AmSettingGroup(
                    label = "앱 설정",
                    items = listOf(
                        {
                            AmSettingItem(title = "상태바 알림", subtitle = "이번달 지출을 상태바에 상시 표시") {
                                Switch(checked = uiState.statusBarEnabled, onCheckedChange = onStatusBarEnabledChange)
                            }
                        },
                        {
                            AmSettingItem(title = "계좌 관리", subtitle = "메인 계좌 등록·수정", onClick = { analytics.logEvent(AmAnalytics.Event.SETTING_ACCOUNT_MANAGE_CLICK); onNavigateToAccount() }) {
                                AmChevron()
                            }
                        },
                        {
                            AmSettingItem(title = "무시 계좌 관리", subtitle = "무시한 출처·계좌 조회·해제", onClick = { analytics.logEvent(AmAnalytics.Event.SETTING_IGNORED_MANAGE_CLICK); onNavigateToIgnored() }) {
                                AmChevron()
                            }
                        },
                    ),
                )
            }

            item {
                AmSettingGroup(
                    label = "정보 · 지원",
                    items = listOf(
                        {
                            AmSettingItem(title = "건의하기", subtitle = "devcoderblue@gmail.com", onClick = { analytics.logEvent(AmAnalytics.Event.SETTING_FEEDBACK_CLICK); onFeedback() }) {
                                AmChevron()
                            }
                        },
                        {
                            AmSettingItem(title = "평가하기", subtitle = "플레이스토어에서 별점 남기기", onClick = { analytics.logEvent(AmAnalytics.Event.SETTING_RATE_CLICK); onRate() }) {
                                AmChevron()
                            }
                        },
                        {
                            AmSettingItem(title = "버전") {
                                Text(versionName.ifBlank { "-" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmColors.TextSecondary)
                            }
                        },
                    ),
                )
            }

            // debug 빌드에서만: 개발자 그룹
            onNavigateToDebug?.let { nav ->
                item {
                    AmSettingGroup(
                        label = "개발자",
                        items = listOf {
                            AmSettingItem(title = "🛠 디버그", subtitle = "개발용 · debug 빌드 전용", onClick = nav) {
                                AmChevron()
                            }
                        },
                    )
                }
            }

            item {
                VerticalSpacer(20.dp)
            }
        }
    }

    if (showTypeDialog) {
        UserTypeDialog(
            current = uiState.userType,
            onSave = { onUserTypeChange(it); showTypeDialog = false },
            onDismiss = { showTypeDialog = false },
        )
    }
    if (showBudgetDialog) {
        BudgetDialog(
            current = uiState.budget,
            label = uiState.userType.label,
            onSave = { onBudgetChange(it); showBudgetDialog = false },
            onDismiss = { showBudgetDialog = false },
        )
    }
    if (showPaydayDialog) {
        PaydayDialog(
            current = uiState.payday,
            title = uiState.userType.paydayLabel,
            onSave = { onPaydayChange(it); showPaydayDialog = false },
            onDismiss = { showPaydayDialog = false },
        )
    }
}

// 후원 강조 카드 — 에메랄드 그라데이션. 글씨 포인트는 기존 설정 행과 동일 크기, 카드만 크게.
@Composable
private fun SupportCard(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "supportScale")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }   // 프레스 스케일(C)
            .clip(AmShape.cardLarge)
            .background(Brush.linearGradient(listOf(AmColors.Emerald, AmColors.EmeraldDark)))
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White),        // 어두운 카드라 흰색 리플
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(AmShape.card)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("☕", fontSize = 24.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("후원하기", style = AmType.bodyStrong, color = Color.White)
            Spacer(Modifier.height(3.dp))
            Text("개발자에게 커피 한 잔 😊", style = AmType.caption, color = Color.White.copy(alpha = 0.85f))
        }
        Box(
            modifier = Modifier
                .clip(AmShape.card)
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text("후원", style = AmType.value, color = AmColors.EmeraldDark)
        }
    }
}

// 유형별 대표 이모지 — 다이얼로그 옵션 카드 좌측 아이콘
private fun UserType.emoji(): String = when (this) {
    UserType.STUDENT -> "👛"
    UserType.YOUTH -> "🛒"
    UserType.COMMON -> "🧮"
}

@Composable
private fun UserTypeDialog(current: UserType, onSave: (UserType) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(current) }
    AmDialog(
        title = "유형 선택",
        onDismiss = onDismiss,
        onConfirm = { onSave(selected) },
        analyticsTag = AmAnalytics.Dialog.USERTYPE,
        analyticsParams = mapOf(AmAnalytics.Param.TYPE to selected.name.lowercase()),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AmSpacing.sm)) {
            UserType.entries.forEach { type ->
                AmSelectableOptionCard(
                    title = type.label,
                    subtitle = type.hint,
                    selected = selected == type,
                    onClick = { selected = type },
                    leading = { Text(type.emoji(), fontSize = 18.sp) },
                )
            }
        }
    }
}

@Composable
private fun BudgetDialog(current: Long, label: String, onSave: (Long) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    val amount = input.toLongOrNull() ?: 0L
    AmDialog(
        title = "월 $label",
        onDismiss = onDismiss,
        onConfirm = { onSave(amount) },
        confirmEnabled = amount > 0,
        analyticsTag = AmAnalytics.Dialog.BUDGET,
    ) {
        AmTextField(
            value = input,
            onValueChange = { v -> input = v.filter { it.isDigit() } },
            label = "월 $label (원)",
            keyboardType = KeyboardType.Number,
            visualTransformation = AmThousandsTransformation(),
        )
    }
}

@Composable
private fun PaydayDialog(current: Int, title: String, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var payday by remember { mutableIntStateOf(current) }
    AmDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { onSave(payday) },
        analyticsTag = AmAnalytics.Dialog.PAYDAY,
        analyticsParams = mapOf(AmAnalytics.Param.PAYDAY to payday),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AmSpacing.md)) {
            // 온보딩과 동일한 칩 구성 (15/20/25/말일)
            Row(horizontalArrangement = Arrangement.spacedBy(AmSpacing.sm)) {
                AmChip("15일", payday == 15) { payday = 15 }
                AmChip("20일", payday == 20) { payday = 20 }
                AmChip("25일", payday == 25) { payday = 25 }
                AmChip("말일", payday == PAYDAY_EOM) { payday = PAYDAY_EOM }
            }
            AmTextField(
                value = if (payday in 1..31) payday.toString() else "",
                onValueChange = { v ->
                    val day = v.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 31)
                    if (day != null) payday = day
                },
                label = "직접 입력 (1~31)",
                keyboardType = KeyboardType.Number,
            )
            // 선택 요약 — 말일처럼 직접입력칸이 비어도 현재 선택을 분명히 표기
            Text("매월 ${paydayLabel(payday)}에 받아요", fontSize = 12.sp, color = AmColors.TextSecondary)
        }
    }
}
