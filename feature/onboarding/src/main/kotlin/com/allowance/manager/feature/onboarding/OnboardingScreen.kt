package com.allowance.manager.feature.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.anim.AmMotion
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.amRippleClickable
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.AmLineTextField
import com.allowance.manager.core.designsystem.component.AmThousandsTransformation
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.domain.model.UserType
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay

private val Accent = AmColors.Emerald
private val ScreenBg = AmColors.CardBg
private val TextPrimary = AmColors.TextPrimary
private val TextSecondary = AmColors.TextSecondary

private const val PAYDAY_EOM = 0
private val BUDGET_PRESETS = listOf(300_000L to "30만원", 500_000L to "50만원", 1_000_000L to "100만원")

@Composable
fun OnboardingRoute(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onFinish()
    }

    val context = LocalContext.current
    val initiallyGranted = remember { isListenerGranted(context) }
    var permissionGranted by remember { mutableStateOf(initiallyGranted) }
    var postGranted by remember { mutableStateOf(isPostNotificationGranted(context)) }
    // 온보딩 내부 스텝: 유형 선택(개인화) → 권한 → 정보 입력.
    var step by remember { mutableStateOf(OnboardingStep.PERSONAL) }

    val openListenerSettings = {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // 알림 보내기(POST) 응답이 끝나면 이어서 알림 접근(리스너) 설정 화면을 연다.
    val postLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        postGranted = granted
        openListenerSettings()
    }

    // 버튼 하나로 두 권한 순차 요청: POST(앱 내 팝업) → 리스너(설정 화면)
    val requestBothPermissions = {
        if (!postGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openListenerSettings()
        }
    }

    // 설정에서 돌아올 때마다 권한 재조회. 거부 시 permissionGranted=false 유지 → 화면 안 넘어감.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = isListenerGranted(context)
        postGranted = isPostNotificationGranted(context)
    }

    // 권한 화면에서 알림 접근이 허용되면 0.5초 뒤에 정보 화면으로 전환 (✓ 상태를 잠깐 노출).
    LaunchedEffect(permissionGranted, step) {
        if (permissionGranted && step == OnboardingStep.PERMISSION) {
            delay(300)
            step = OnboardingStep.INFO
        }
    }

    // 온보딩 화면 이동은 공통 가로 슬라이드 전환 사용.
    AnimatedContent(
        targetState = step,
        transitionSpec = { AmMotion.slideForward() },
        label = "onboardingStep",
    ) { current ->
        when (current) {
            OnboardingStep.PERSONAL -> PersonalOnboardingScreen(
                selected = uiState.userType,
                onSelect = viewModel::onUserTypeChange,
                onNext = {
                    // 유형 확정 → 저장 후 다음 스텝 (권한 이미 있으면 정보로 바로)
                    viewModel.confirmUserType()
                    step = if (permissionGranted) OnboardingStep.INFO else OnboardingStep.PERMISSION
                },
            )
            OnboardingStep.PERMISSION -> OnboardingPermissionScreen(
                postGranted = postGranted,
                listenerGranted = permissionGranted,
                onAllow = requestBothPermissions,
            )
            OnboardingStep.INFO -> OnboardingInfoScreen(
                uiState = uiState,
                onBankNameChange = viewModel::onBankNameChange,
                onAccountPatternChange = viewModel::onAccountPatternChange,
                onBudgetChange = viewModel::onBudgetChange,
                onPaydayInputChange = viewModel::onPaydayInputChange,
                onPaydaySelect = viewModel::onPaydaySelect,
                // 기본 정보 입력 완료 → 알림 설정 스텝으로
                onNext = { step = OnboardingStep.ALERT },
            )
            OnboardingStep.ALERT -> OnboardingAlertScreen(
                uiState = uiState,
                onBudgetAlertToggle = viewModel::onBudgetAlertToggle,
                onBudgetAlertFrequencyChange = viewModel::onBudgetAlertFrequencyChange,
                onDailyReminderToggle = viewModel::onDailyReminderToggle,
                onReminderTimeChange = viewModel::onReminderTimeChange,
                onFinish = viewModel::finish,
            )
        }
    }
}

private enum class OnboardingStep { PERSONAL, PERMISSION, INFO, ALERT }

// ── 유형 선택 (온보딩 첫 단계) ─────────────────────────
// 뱃지(용돈/생활비/예산) + 아래 설명. 선택 후 '다음'에서 유형이 저장·고정된다.
@Composable
private fun PersonalOnboardingScreen(
    selected: UserType,
    onSelect: (UserType) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(ScreenBg).padding(horizontal = 24.dp).padding(top = 30.dp, bottom = 24.dp),
    ) {
        Text("어떤 단어가 좋으신가요?", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("앱에서 사용할 편한 호칭을 골라주세요.", fontSize = 16.sp, color = TextSecondary)
        Spacer(Modifier.height(36.dp))

        Column(verticalArrangement = Arrangement.spacedBy(AmSpacing.md)) {
            UserType.entries.forEach { type ->
                UserTypeOption(type = type, selected = selected == type, onClick = { onSelect(type) })
            }
        }

        Spacer(Modifier.weight(1f))
        AmButton(text = "다음", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun UserTypeOption(type: UserType, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AmShape.card)
            .border(1.5.dp, if (selected) Accent else AmColors.BarTrack, AmShape.card)
            .background(if (selected) AmColors.EmeraldBg else AmColors.CardBg)
            .amRippleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // 뱃지 (호칭)
        Text(
            text = type.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) Accent else TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(type.hint, fontSize = 13.sp, color = TextSecondary)
    }
}

// ── 권한 화면 (알림 접근 권한 없을 때) ─────────────────────────
// 두 권한을 함께 안내하고, 하단 버튼 하나로 순차 요청.
@Composable
fun OnboardingPermissionScreen(
    postGranted: Boolean,
    listenerGranted: Boolean,
    onAllow: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(ScreenBg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text("🔔", fontSize = 56.sp)
        Spacer(Modifier.height(24.dp))
        Text("필수 권한 허용이 필요해요", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "앱 사용을 위해 아래 두 권한이 필요해요.\n금융 정보는 기기에만 저장돼요.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        PermissionRow(
            emoji = "📩",
            title = "알림 보내기",
            desc = "상태바에 남은 예산을 항상 표시해요",
            granted = postGranted,
        )
        Spacer(Modifier.height(12.dp))
        PermissionRow(
            emoji = "💳",
            title = "알림 접근",
            desc = "카드·은행 결제 알림을 읽어 자동 기록해요",
            granted = listenerGranted,
        )
        Spacer(Modifier.weight(1f))
        AmButton(
            text = "권한 허용하기",
            onClick = onAllow,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PermissionRow(
    emoji: String,
    title: String,
    desc: String,
    granted: Boolean,
) {
    AmCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(AmSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(desc, fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(Modifier.width(AmSpacing.md))
            Text(
                text = if (granted) "✓ 허용됨" else "필요",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (granted) Accent else TextSecondary,
            )
        }
    }
}

// ── 정보 화면 (월급일·용돈·계좌, 위에서 쌓이는 애니메이션) ──────
@Composable
fun OnboardingInfoScreen(
    uiState: OnboardingUiState,
    onBankNameChange: (String) -> Unit = {},
    onAccountPatternChange: (String) -> Unit = {},
    onBudgetChange: (String) -> Unit = {},
    onPaydayInputChange: (String) -> Unit = {},
    onPaydaySelect: (Int) -> Unit = {},
    onNext: () -> Unit = {},
) {
    // 자동 진행: 월급일 확정 → 용돈 노출 → 용돈 확정 → 계좌(선택) + 시작하기 노출.
    // 새 항목은 위에 추가되고 기존은 아래로 밀림.
    // payday는 기본값이 있어 값 유무로 판정 불가 → 사용자 동작(칩 탭 / 키보드 '다음')으로 확정 처리.
    var paydayDone by remember { mutableStateOf(false) }
    var budgetDone by remember { mutableStateOf(false) }

    // 단계가 넘어갈 때 이전 입력칸의 포커스를 풀어 키보드를 내린다.
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            // 빈 곳(입력칸·칩·버튼 외)을 탭하면 포커스 해제 → 키보드 내림
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .padding(horizontal = 24.dp)
            .padding(top = 30.dp),
    ) {
        Text("기본 정보를 입력해요", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("설정에서 언제든 바꿀 수 있어요.", fontSize = 16.sp, color = TextSecondary)
        Spacer(Modifier.height(36.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            // 세번째 : 계좌 — 용돈 확정 후 노출
            AnimatedVisibility(visible = budgetDone, enter = fadeIn() + expandVertically()) {
                AccountSection(
                    bankName = uiState.bankName,
                    accountPattern = uiState.accountPattern,
                    label = uiState.userType.label,
                    onBankNameChange = onBankNameChange,
                    onAccountPatternChange = onAccountPatternChange,
                )
            }
            // 두번째 : 용돈 — 월급일 확정 후 노출
            AnimatedVisibility(visible = paydayDone, enter = fadeIn() + expandVertically()) {
                BudgetSection(
                    budgetInput = uiState.budgetInput,
                    budget = uiState.budget,
                    label = uiState.userType.label,
                    topic = uiState.userType.topic,
                    onBudgetChange = onBudgetChange,
                    // IME '완료'·칩: 키보드 내리고 단계 완료
                    onCommit = { focusManager.clearFocus(); budgetDone = true },
                    // 포커스 이탈(다른 필드 탭): 단계만 완료 — clearFocus로 새 필드 포커스를 뺏지 않는다
                    onStepDone = { budgetDone = true },
                )
            }
            // 첫번째 : 월급일 — 항상 노출
            PaydaySection(
                payday = uiState.payday,
                paydayInput = uiState.paydayInput,
                paydayLabel = uiState.userType.paydayLabel,
                onInputChange = onPaydayInputChange,
                onSelect = onPaydaySelect,
                onCommit = { focusManager.clearFocus(); paydayDone = true },
                onStepDone = { paydayDone = true },
            )
        }

        // 마지막 단계까지 왔을 때 노출. 계좌까지 모두 채워야 활성화(canFinish).
        AnimatedVisibility(visible = budgetDone, enter = fadeIn() + expandVertically()) {
            Column {
                Spacer(Modifier.height(AmSpacing.md))
                AmButton(
                    text = "다음",
                    onClick = onNext,
                    enabled = uiState.canFinish,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    AmCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 0.dp),
    ) {
        Column {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(Modifier.height(AmSpacing.sm + 2.dp))
            content()
        }
    }
}

@Composable
private fun AccountSection(
    bankName: String,
    accountPattern: String,
    label: String,
    onBankNameChange: (String) -> Unit,
    onAccountPatternChange: (String) -> Unit,
) {
    Section("$label 계좌 정보를 입력해주세요") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("은행 이름과 계좌 번호로 입출금 알림을 자동 감지해요.", fontSize = 14.sp, color = TextSecondary)
            AmLineTextField(
                value = bankName,
                onValueChange = onBankNameChange,
                hint = "예) 국민은행",
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth(),
            )
            AmLineTextField(
                value = accountPattern,
                // 숫자와 구분자(-)만 입력 허용. 저장 시 도메인에서 숫자만 남긴다.
                onValueChange = { v -> onAccountPatternChange(v.filter { it.isDigit() || it == '-' }) },
                hint = "예) 941111-11-111111 (\"-\" 제외)",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BudgetSection(
    budgetInput: String,
    budget: Long,
    label: String,
    topic: String,
    onBudgetChange: (String) -> Unit,
    onCommit: () -> Unit,
    onStepDone: () -> Unit,
) {
    Section("월 $label$topic 얼마인가요?") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AmLineTextField(
                value = budgetInput,
                onValueChange = { onBudgetChange(it.filter { c -> c.isDigit() }) },
                hint = "예) 500,000",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                // 값이 있을 때만 다음 단계로 (입력 중 조기 진행 방지)
                onImeAction = { if (budget > 0) onCommit() },
                // 포커스 이탈은 단계 완료만 — clearFocus 금지(다음 필드 포커스 보존)
                onFocusLost = { if (budget > 0) onStepDone() },
                visualTransformation = remember { AmThousandsTransformation() },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BUDGET_PRESETS.forEach { (amount, label) ->
                    // 프리셋은 항상 유효 → 바로 다음 단계
                    AmChip(label = label, selected = budget == amount) {
                        onBudgetChange(amount.toString())
                        onCommit()
                    }
                }
            }
        }
    }
}

@Composable
private fun PaydaySection(
    payday: Int?,
    paydayInput: String,
    paydayLabel: String,
    onInputChange: (String) -> Unit,
    onSelect: (Int) -> Unit,
    onCommit: () -> Unit,
    onStepDone: () -> Unit,
) {
    Section("${paydayLabel}이 언제인가요?") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            AmLineTextField(
                // 텍스트는 입력값을 그대로 표시 → 자유롭게 지우고 수정 가능(스냅백 없음)
                value = paydayInput,
                onValueChange = onInputChange,
                hint = "예) 25",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                // 값이 유효(1~31)할 때만 다음 단계로
                onImeAction = { if (payday != null) onCommit() },
                // 포커스 이탈은 단계 완료만 — clearFocus 금지(다음 필드 포커스 보존)
                onFocusLost = { if (payday != null) onStepDone() },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 칩 선택 → 입력 텍스트도 함께 채움. 이후 숫자 입력 시 payday가 바뀌면 자동 해제
                AmChip("15일", payday == 15) { onSelect(15); onCommit() }
                AmChip("20일", payday == 20) { onSelect(20); onCommit() }
                AmChip("25일", payday == 25) { onSelect(25); onCommit() }
                AmChip("말일", payday == PAYDAY_EOM) { onSelect(PAYDAY_EOM); onCommit() }
            }
        }
    }
}

private fun isListenerGranted(context: android.content.Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

// POST_NOTIFICATIONS는 Android 13(TIRAMISU)+에서만 런타임 권한. 그 이하는 항상 허용된 것으로 간주.
private fun isPostNotificationGranted(context: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
