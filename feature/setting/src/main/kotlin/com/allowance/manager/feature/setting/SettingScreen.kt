package com.allowance.manager.feature.setting

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
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
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmChevron
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.amRippleClickable
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.LocalAnalyticsHelper
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmScreenHeader
import com.allowance.manager.core.designsystem.component.AmSelectableOptionCard
import com.allowance.manager.core.designsystem.component.AmSettingGroup
import com.allowance.manager.core.designsystem.component.AmSettingItem
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.component.AmThousandsTransformation
import com.allowance.manager.core.designsystem.component.AmTimePickerDialog
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.AlertFrequency
import java.time.DayOfWeek
import java.time.YearMonth
import com.allowance.manager.core.domain.model.PaydayAlertSetting
import com.allowance.manager.core.domain.model.PaydayInfo
import com.allowance.manager.core.domain.model.PaydayWarning
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.domain.util.formatTimeOfDay
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
    // 설치된 앱의 versionName·versionCode를 그대로 표시 (모듈 BuildConfig 결합 없이)
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }
    val versionCode = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION") // API 28 미만 fallback (minSdk 26)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        }.getOrNull() ?: 0L
    }

    // 후원(인앱 결제) — 화면 생존 동안 클라이언트 유지, 이탈 시 연결 해제
    val donationManager = remember { DonationManager(context) }
    DisposableEffect(Unit) { onDispose { donationManager.release() } }
    var showThanks by remember { mutableStateOf(false) }

    SettingScreen(
        uiState = uiState,
        versionName = versionName,
        versionCode = versionCode,
        onBack = onBack,
        onNavigateToAccount = onNavigateToAccount,
        onNavigateToIgnored = onNavigateToIgnored,
        onNavigateToDebug = onNavigateToDebug,
        onStatusBarEnabledChange = viewModel::setStatusBarEnabled,
        onBudgetChange = viewModel::setBudget,
        onPaydayChange = viewModel::setPayday,
        onUserTypeChange = viewModel::setUserType,
        onBudgetAlertEnabledChange = viewModel::setBudgetAlertEnabled,
        onBudgetAlertFrequencyChange = viewModel::setBudgetAlertFrequency,
        onDailyReminderEnabledChange = viewModel::setDailyReminderEnabled,
        onReminderTimeChange = viewModel::setDailyReminderTime,
        onPaydayAlertEnabledChange = viewModel::setPaydayAlertEnabled,
        onPaydayOverrideSave = viewModel::setPaydayOverride,
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
                "이 한 잔이 저에게는 큰 힘이 됩니다.\n더 쓸 만한 가계부로 보답하겠습니다!",
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
    versionCode: Long = 0,
    onBack: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToIgnored: () -> Unit = {},
    onNavigateToDebug: (() -> Unit)? = null,
    onStatusBarEnabledChange: (Boolean) -> Unit = {},
    onBudgetChange: (Long) -> Unit = {},
    onPaydayChange: (Int) -> Unit = {},
    onUserTypeChange: (UserType) -> Unit = {},
    onBudgetAlertEnabledChange: (Boolean) -> Unit = {},
    onBudgetAlertFrequencyChange: (AlertFrequency) -> Unit = {},
    onDailyReminderEnabledChange: (Boolean) -> Unit = {},
    onReminderTimeChange: (Int, Int) -> Unit = { _, _ -> },
    onPaydayAlertEnabledChange: (Boolean) -> Unit = {},
    onPaydayOverrideSave: (YearMonth, Int?) -> Unit = { _, _ -> },
    onSupport: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onRate: () -> Unit = {},
) {
    val analytics = LocalAnalyticsHelper.current
    // 다이얼로그 노출 여부는 순수 UI 상태 → 화면이 직접 보유
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showPaydayDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    // 이번 달 월급일 조정 — 다이얼로그를 열 때의 PaydayInfo를 고정해 자정을 넘겨도 대상 달이 바뀌지 않게 한다
    var paydayOverrideTarget by remember { mutableStateOf<PaydayInfo?>(null) }

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
                            AmSettingItem(title = "유형", subtitle = "금액을 부르는 호칭 (용돈/생활비/예산)", onClick = { showTypeDialog = true }) {
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
                        {
                            // 이번 달만 실제 받은 날로 조정 — 값은 실지급일(공휴일·주말 보정 또는 조정값)
                            AmSettingItem(
                                title = "이번 달 ${uiState.userType.paydayShort} 조정",
                                onClick = { uiState.paydayInfo?.let { paydayOverrideTarget = it } },
                            ) {
                                Text(
                                    uiState.paydayInfo?.actual?.let { "${it.monthValue}월 ${it.dayOfMonth}일" } ?: "",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmColors.Emerald,
                                )
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
                AlertSettingsGroup(
                    budgetAlertEnabled = uiState.budgetAlert.enabled,
                    frequencyLabel = uiState.budgetAlert.frequency.label,
                    frequencySummary = uiState.budgetAlert.frequency.summary,
                    reminderEnabled = uiState.dailyReminder.enabled,
                    reminderTime = formatTimeOfDay(uiState.dailyReminder.hour, uiState.dailyReminder.minute),
                    paydayAlertEnabled = uiState.paydayAlert.enabled,
                    paydayAlertTime = formatTimeOfDay(PaydayAlertSetting.HOUR, PaydayAlertSetting.MINUTE),
                    paydayShort = uiState.userType.paydayShort,
                    onPaydayAlertEnabledChange = onPaydayAlertEnabledChange,
                    onBudgetAlertEnabledChange = onBudgetAlertEnabledChange,
                    onDailyReminderEnabledChange = onDailyReminderEnabledChange,
                    onFrequencyClick = { showFrequencyDialog = true },
                    onTimeClick = { showTimePicker = true },
                )
            }

            item {
                AmSettingGroup(
                    label = "정보 · 지원",
                    items = listOf(
                        {
                            AmSettingItem(title = "건의하기", onClick = { analytics.logEvent(AmAnalytics.Event.SETTING_FEEDBACK_CLICK); onFeedback() }) {
                                AmChevron()
                            }
                        },
                        {
                            AmSettingItem(title = "평가하기", onClick = { analytics.logEvent(AmAnalytics.Event.SETTING_RATE_CLICK); onRate() }) {
                                AmChevron()
                            }
                        },
                        {
                            AmSettingItem(title = "버전") {
                                // 버전명 + 빌드번호(versionCode) 함께 표기: "1.0.0 (3)"
                                val versionLabel = if (versionName.isBlank()) "-" else "$versionName ($versionCode)"
                                Text(versionLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmColors.TextSecondary)
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
    paydayOverrideTarget?.let { info ->
        PaydayOverrideDialog(
            info = info,
            paydayName = uiState.userType.paydayShort,
            onSave = { day -> onPaydayOverrideSave(info.month, day); paydayOverrideTarget = null },
            onDismiss = { paydayOverrideTarget = null },
        )
    }

    if (showFrequencyDialog) {
        FrequencyDialog(
            current = uiState.budgetAlert.frequency,
            onSave = { onBudgetAlertFrequencyChange(it); showFrequencyDialog = false },
            onDismiss = { showFrequencyDialog = false },
        )
    }
    if (showTimePicker) {
        AmTimePickerDialog(
            initialHour = uiState.dailyReminder.hour,
            initialMinute = uiState.dailyReminder.minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute -> onReminderTimeChange(hour, minute); showTimePicker = false },
        )
    }
}

// 단일 "알림" 그룹 — 두 기능을 한 카드에 담고, 각 기능의 세부 옵션(빈도/시각)을
// 들여쓴 하위 행으로 매단다. 토글 OFF면 그 기능의 세부 행만 사라진다.
@Composable
private fun AlertSettingsGroup(
    budgetAlertEnabled: Boolean,
    frequencyLabel: String,
    frequencySummary: String,
    reminderEnabled: Boolean,
    reminderTime: String,
    paydayAlertEnabled: Boolean,
    paydayAlertTime: String,
    paydayShort: String,
    onBudgetAlertEnabledChange: (Boolean) -> Unit,
    onDailyReminderEnabledChange: (Boolean) -> Unit,
    onPaydayAlertEnabledChange: (Boolean) -> Unit,
    onFrequencyClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "알림",
            style = AmType.size12_black,
            color = AmColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 11.dp),
        )
        AmCard(
            modifier = Modifier.fillMaxWidth(),
            shape = AmShape.cardSmall,
            contentPadding = PaddingValues(0.dp),
        ) {
            Column {
                AlertFeatureRow(title = "예산 소진 알림", checked = budgetAlertEnabled, onCheckedChange = onBudgetAlertEnabledChange, hasDetail = budgetAlertEnabled)
                if (budgetAlertEnabled) {
                    AlertDetailRow(label = "빈도", subtitle = frequencySummary, value = frequencyLabel, onClick = onFrequencyClick)
                }
                HorizontalDivider(thickness = 1.dp, color = AmColors.Divider)
                AlertFeatureRow(title = "가계부 관리 알림", checked = reminderEnabled, onCheckedChange = onDailyReminderEnabledChange, hasDetail = reminderEnabled)
                if (reminderEnabled) {
                    AlertDetailRow(label = "알림 시각", subtitle = "", value = reminderTime, onClick = onTimeClick)
                }
                HorizontalDivider(thickness = 1.dp, color = AmColors.Divider)
                AlertFeatureRow(title = "$paydayShort 알림", checked = paydayAlertEnabled, onCheckedChange = onPaydayAlertEnabledChange, hasDetail = paydayAlertEnabled)
                if (paydayAlertEnabled) {
                    // 시각은 고정이라 누를 수 없다 — 공휴일 보정된 실지급일 기준이라는 점만 알려준다
                    AlertDetailRow(
                        label = "전날 · 당일",
                        subtitle = "공휴일이면 실제 지급일 기준으로 알려드려요",
                        value = paydayAlertTime,
                        onClick = null,
                    )
                }
            }
        }
    }
}

// 기능 on/off 행(제목 + 스위치). 아래 세부 블록이 있으면 하단 여백을 줄여 밀착시킨다.
@Composable
private fun AlertFeatureRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, hasDetail: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = if (hasDetail) 8.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = AmType.size14_bold, color = AmColors.TextPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// 기능에 딸린 세부 옵션 — 양옆 여백을 둔 연초록 인셋 블록으로 상위 기능 종속을 표현.
// 블록의 여백·색이 들여쓰기 역할을 하므로 텍스트를 추가로 들여쓰지 않는다.
@Composable
private fun AlertDetailRow(label: String, subtitle: String, value: String, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 10.dp)
            .clip(AmShape.cardSmall)
            .background(AmColors.EmeraldBg)
            .let { if (onClick != null) it.amRippleClickable(onClick = onClick) else it }
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AmColors.EmeraldDark)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 10.sp, color = AmColors.EmeraldMid)
            }
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmColors.EmeraldDark)
        if (onClick != null) {
            Spacer(Modifier.width(AmSpacing.xs))
            Text("›", fontSize = 18.sp, color = AmColors.EmeraldDark)
        }
    }
}

/**
 * 이번 달만 지급일을 바꾸는 다이얼로그.
 *
 * - 비우고 저장하면 지정 해제(규칙일로 복귀) — 그래서 힌트에 기본일을 적어둔다.
 * - 입력한 날이 공휴일·주말이면 경고만 띄우고 **저장은 막지 않는다.** 실제로 그날 받았을 수 있고,
 *   지정값에는 영업일 보정도 걸지 않는다.
 */
@Composable
private fun PaydayOverrideDialog(
    info: PaydayInfo,
    paydayName: String,
    onSave: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf(info.overrideDay?.toString() ?: "") }
    val day = input.toIntOrNull()?.takeIf { it in 1..31 }
    val warning = day?.let { info.warningFor(it) }
    AmDialog(
        title = "이번 달 $paydayName 조정",
        onDismiss = onDismiss,
        onConfirm = { onSave(day) },
        analyticsTag = AmAnalytics.Dialog.PAYDAY_OVERRIDE,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AmSpacing.md)) {
            Text(
                buildString {
                    appendLine("${info.month.monthValue}월에만 적용돼요. 다음 달부터는 다시 ${paydayRuleLabel(info.rule)}로 계산해요.")
                    append("홈에 보이는 이번 달 기간과 지출도 함께 바뀌어요.")
                },
                fontSize = 12.sp,
                color = AmColors.TextSecondary,
            )
            AmTextField(
                value = input,
                onValueChange = { v -> input = v.filter { it.isDigit() }.take(2) },
                label = "기본 ${paydayLabel(info.rule)}",
                keyboardType = KeyboardType.Number,
                supportingText = warning?.message(info.month.monthValue),
                supportingTextColor = AmColors.Red,
            )
        }
    }
}

/** 경고 문구 — 이름을 알면 "8월 31일은 추석이에요", 모르면 "…은 공휴일이에요" */
private fun PaydayWarning.message(month: Int): String {
    val dayText = "${month}월 ${date.dayOfMonth}일"
    return when (kind) {
        PaydayWarning.Kind.HOLIDAY -> "${dayText}은 ${holidayName ?: "공휴일"}이에요"
        PaydayWarning.Kind.WEEKEND ->
            if (date.dayOfWeek == DayOfWeek.SATURDAY) "${dayText}은 토요일이에요" else "${dayText}은 일요일이에요"
    }
}

/** "매월 25일" / "매월 말일" — 조정 다이얼로그 안내문에서 규칙일을 부를 때 */
private fun paydayRuleLabel(rule: Int): String = "매월 ${paydayLabel(rule)}"

@Composable
private fun FrequencyDialog(current: AlertFrequency, onSave: (AlertFrequency) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(current) }
    AmDialog(
        title = "알림 빈도",
        onDismiss = onDismiss,
        onConfirm = { onSave(selected) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AmSpacing.sm)) {
            AlertFrequency.entries.forEach { freq ->
                AmSelectableOptionCard(
                    title = freq.displayLabel,
                    subtitle = freq.summary,
                    selected = selected == freq,
                    onClick = { selected = freq },
                    subtitleFontSize = 11.sp,
                )
            }
        }
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
            Text("후원하기", style = AmType.size14_bold, color = Color.White)
            Spacer(Modifier.height(3.dp))
            Text("개발자에게 커피 한 잔 😊", style = AmType.size11_medium, color = Color.White.copy(alpha = 0.85f))
        }
        Box(
            modifier = Modifier
                .clip(AmShape.card)
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text("후원", style = AmType.size13_bold, color = AmColors.EmeraldDark)
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
