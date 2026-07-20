package com.allowance.manager.feature.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.allowance.manager.core.ui.theme.AmColors

private val Accent = AmColors.Emerald
private val CardBg = AmColors.CardBg
private val ScreenBg = AmColors.CardBg
private val ChipBg = AmColors.ChipBg
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
    var permissionGranted by remember { mutableStateOf(isListenerGranted(context)) }
    var skippedPermission by remember { mutableStateOf(false) }

    // 설정에서 돌아올 때마다 권한 재조회 → 허용되면 자동으로 정보 화면으로
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = isListenerGranted(context)
    }

    if (permissionGranted || skippedPermission) {
        InfoScreen(
            uiState = uiState,
            onBankNameChange = viewModel::onBankNameChange,
            onAccountPatternChange = viewModel::onAccountPatternChange,
            onBudgetChange = viewModel::onBudgetChange,
            onPaydayChange = viewModel::onPaydayChange,
            onFinish = viewModel::finish,
        )
    } else {
        PermissionScreen(onSkip = { skippedPermission = true })
    }
}

// ── 권한 화면 (권한 없을 때만) ─────────────────────────────
@Composable
private fun PermissionScreen(onSkip: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(ScreenBg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text("💳", fontSize = 56.sp)
        Spacer(Modifier.height(24.dp))
        Text("알림 접근 권한이 필요해요", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "카드·은행 결제 알림을 자동으로 읽어 가계부를 채워요.\n금융 정보는 기기에만 저장돼요.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("권한 설정 열기") }
        Spacer(Modifier.height(8.dp))
        Text("나중에 설정할게요", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.clickable(onClick = onSkip).padding(8.dp))
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

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { if (isLast) onFinish() else revealed++ },
            enabled = if (isLast) uiState.canFinish else true,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (isLast) "시작하기" else "다음") }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp),
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Spacer(Modifier.height(10.dp))
        content()
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
            OutlinedTextField(
                value = bankName,
                onValueChange = onBankNameChange,
                label = { Text("은행/앱 이름 (예: 신한은행)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = accountPattern,
                onValueChange = onAccountPatternChange,
                label = { Text("계좌 패턴 (예: 941602-**-***318)") },
                singleLine = true,
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
            OutlinedTextField(
                value = budgetInput,
                onValueChange = { onBudgetChange(it.filter { c -> c.isDigit() }) },
                label = { Text("월 예산 (원)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BUDGET_PRESETS.forEach { (amount, label) ->
                    OptionChip(label = label, selected = budget == amount) { onBudgetChange(amount.toString()) }
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
                OptionChip("15일", payday == 15) { onPaydayChange(15) }
                OptionChip("25일", payday == 25) { onPaydayChange(25) }
                OptionChip("말일", payday == PAYDAY_EOM) { onPaydayChange(PAYDAY_EOM) }
            }
            OutlinedTextField(
                value = if (payday in 1..31) payday.toString() else "",
                onValueChange = { v ->
                    val day = v.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 31)
                    if (day != null) onPaydayChange(day)
                },
                label = { Text("직접 입력 (1~31)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Accent else ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(label, color = if (selected) Color.White else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

private fun isListenerGranted(context: android.content.Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
