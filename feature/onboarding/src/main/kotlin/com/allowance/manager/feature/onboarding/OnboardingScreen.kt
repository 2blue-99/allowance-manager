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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmSpacing
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
    // 권한 허용 직후 바로 넘어가지 않고, ✓ 상태를 잠깐 보여준 뒤 전환.
    var showInfo by remember { mutableStateOf(initiallyGranted) }

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

    // 알림 접근이 허용되면 0.5초 뒤에 정보 화면으로 전환 (✓ 상태를 잠깐 노출).
    LaunchedEffect(permissionGranted) {
        if (permissionGranted && !showInfo) {
            delay(500)
            showInfo = true
        }
    }

    // 온보딩 화면 이동은 공통 가로 슬라이드 전환 사용.
    AnimatedContent(
        targetState = showInfo,
        transitionSpec = { AmMotion.slideForward() },
        label = "onboardingStep",
    ) { onInfo ->
        if (onInfo) {
            InfoScreen(
                uiState = uiState,
                onBankNameChange = viewModel::onBankNameChange,
                onAccountPatternChange = viewModel::onAccountPatternChange,
                onBudgetChange = viewModel::onBudgetChange,
                onPaydayChange = viewModel::onPaydayChange,
                onFinish = viewModel::finish,
            )
        } else {
            PermissionScreen(
                postGranted = postGranted,
                listenerGranted = permissionGranted,
                onAllow = requestBothPermissions,
            )
        }
    }
}

// ── 권한 화면 (알림 접근 권한 없을 때) ─────────────────────────
// 두 권한을 함께 안내하고, 하단 버튼 하나로 순차 요청.
@Composable
private fun PermissionScreen(
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

// ── 정보 화면 (계좌·예산·수급일, 위에서 쌓이는 애니메이션) ──────
@Composable
private fun InfoScreen(
    uiState: OnboardingUiState,
    onBankNameChange: (String) -> Unit,
    onAccountPatternChange: (String) -> Unit,
    onBudgetChange: (String) -> Unit,
    onPaydayChange: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    // 1=계좌, 2=+예산, 3=+수급일. 새 항목은 위에 추가되고 기존은 아래로 밀림.
    var revealed by remember { mutableIntStateOf(1) }
    val isLast = revealed >= 3

    Column(
        modifier = Modifier.fillMaxSize().background(ScreenBg).padding(24.dp),
    ) {
        Text("기본 정보를 입력해요", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("설정에서 언제든 바꿀 수 있어요.", fontSize = 13.sp, color = TextSecondary)
        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // 위(최신) → 아래(기존): 수급일 → 예산 → 계좌
            AnimatedVisibility(visible = revealed >= 3, enter = fadeIn() + expandVertically()) {
                PaydaySection(payday = uiState.payday, onPaydayChange = onPaydayChange)
            }
            AnimatedVisibility(visible = revealed >= 2, enter = fadeIn() + expandVertically()) {
                BudgetSection(budgetInput = uiState.budgetInput, budget = uiState.budget, onBudgetChange = onBudgetChange)
            }
            AnimatedVisibility(visible = revealed >= 1, enter = fadeIn() + expandVertically()) {
                AccountSection(
                    bankName = uiState.bankName,
                    accountPattern = uiState.accountPattern,
                    onBankNameChange = onBankNameChange,
                    onAccountPatternChange = onAccountPatternChange,
                )
            }
        }

        Spacer(Modifier.height(AmSpacing.md))
        AmButton(
            text = if (isLast) "시작하기" else "다음",
            onClick = { if (isLast) onFinish() else revealed++ },
            enabled = if (isLast) uiState.canFinish else true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    AmCard(modifier = Modifier.fillMaxWidth().padding(bottom = AmSpacing.md)) {
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(Modifier.height(AmSpacing.sm + 2.dp))
            content()
        }
    }
}

@Composable
private fun AccountSection(
    bankName: String,
    accountPattern: String,
    onBankNameChange: (String) -> Unit,
    onAccountPatternChange: (String) -> Unit,
) {
    Section("계좌 등록 (선택)") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AmTextField(
                value = bankName,
                onValueChange = onBankNameChange,
                label = "은행/앱 이름 (예: 신한은행)",
                modifier = Modifier.fillMaxWidth(),
            )
            AmTextField(
                value = accountPattern,
                onValueChange = onAccountPatternChange,
                label = "계좌 패턴 (예: 941602-**-***318)",
                modifier = Modifier.fillMaxWidth(),
            )
            Text("* 나중에 홈에서 감지된 거래로도 등록할 수 있어요.", fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun BudgetSection(budgetInput: String, budget: Long, onBudgetChange: (String) -> Unit) {
    Section("이번달 예산") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AmTextField(
                value = budgetInput,
                onValueChange = { onBudgetChange(it.filter { c -> c.isDigit() }) },
                label = "월 예산 (원)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BUDGET_PRESETS.forEach { (amount, label) ->
                    AmChip(label = label, selected = budget == amount) { onBudgetChange(amount.toString()) }
                }
            }
        }
    }
}

@Composable
private fun PaydaySection(payday: Int, onPaydayChange: (Int) -> Unit) {
    Section("수급일") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmChip("15일", payday == 15) { onPaydayChange(15) }
                AmChip("25일", payday == 25) { onPaydayChange(25) }
                AmChip("말일", payday == PAYDAY_EOM) { onPaydayChange(PAYDAY_EOM) }
            }
            AmTextField(
                value = if (payday in 1..31) payday.toString() else "",
                onValueChange = { v ->
                    val day = v.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 31)
                    if (day != null) onPaydayChange(day)
                },
                label = "직접 입력 (1~31)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth(),
            )
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
