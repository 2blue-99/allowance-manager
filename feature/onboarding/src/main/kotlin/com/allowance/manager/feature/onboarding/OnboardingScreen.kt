package com.allowance.manager.feature.onboarding

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

private val Accent = Color(0xFF10B981)
private val PlaceholderBg = Color(0xFFECECEC)
private val TextSecondary = Color(0xFF8A97AA)

private const val PAGE_COUNT = 4
private const val PAYDAY_EOM = 0 // 말일

@Composable
fun OnboardingRoute(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onFinish()
    }

    OnboardingScreen(
        uiState = uiState,
        onBankNameChange = viewModel::onBankNameChange,
        onAccountPatternChange = viewModel::onAccountPatternChange,
        onBudgetChange = viewModel::onBudgetChange,
        onPaydayChange = viewModel::onPaydayChange,
        onFinish = viewModel::finish,
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onBankNameChange: (String) -> Unit = {},
    onAccountPatternChange: (String) -> Unit = {},
    onBudgetChange: (String) -> Unit = {},
    onPaydayChange: (Int) -> Unit = {},
    onFinish: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            when (page) {
                0 -> PermissionSlide()
                1 -> AccountSlide(
                    bankName = uiState.bankName,
                    accountPattern = uiState.accountPattern,
                    onBankNameChange = onBankNameChange,
                    onAccountPatternChange = onAccountPatternChange,
                )
                2 -> BudgetSlide(budgetInput = uiState.budgetInput, onBudgetChange = onBudgetChange)
                3 -> PaydaySlide(payday = uiState.payday, onPaydayChange = onPaydayChange)
            }
        }

        PageIndicator(current = pagerState.currentPage)
        Spacer(Modifier.height(16.dp))

        val isLast = pagerState.currentPage == PAGE_COUNT - 1
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) { Text("이전", color = TextSecondary) }
            }
            Spacer(Modifier.weight(1f))
            if (isLast) {
                Button(onClick = onFinish, enabled = uiState.canFinish) { Text("시작하기") }
            } else {
                Button(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) { Text("다음") }
            }
        }
    }
}

// ── 슬라이드 ─────────────────────────────────────────

@Composable
private fun SlideScaffold(
    title: String,
    description: String,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ImagePlaceholder()
        Spacer(Modifier.height(28.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            description,
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
private fun ImagePlaceholder() {
    // TODO: 나중에 실제 이미지로 교체
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(PlaceholderBg),
        contentAlignment = Alignment.Center,
    ) {
        Text("이미지", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun PermissionSlide() {
    val context = LocalContext.current
    SlideScaffold(
        title = "알림 접근 권한이 필요해요",
        description = "카드·은행 결제 알림을 자동으로 읽어 가계부를 채워요.\n금융 정보는 기기에만 저장돼요.",
    ) {
        OutlinedButton(onClick = {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }) { Text("권한 설정 열기") }
    }
}

@Composable
private fun AccountSlide(
    bankName: String,
    accountPattern: String,
    onBankNameChange: (String) -> Unit,
    onAccountPatternChange: (String) -> Unit,
) {
    SlideScaffold(
        title = "내 계좌를 등록해요",
        description = "알림에 보이는 계좌번호 일부(마스킹)를 입력하면\n그 계좌 거래만 예산에 반영돼요.",
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            Text("* 지금 건너뛰어도 나중에 추가할 수 있어요.", fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun BudgetSlide(budgetInput: String, onBudgetChange: (String) -> Unit) {
    SlideScaffold(
        title = "이번달 예산을 정해요",
        description = "이번달 사이클 동안 쓸 수 있는 금액이에요.",
    ) {
        OutlinedTextField(
            value = budgetInput,
            onValueChange = { onBudgetChange(it.filter { c -> c.isDigit() }) },
            label = { Text("월 예산 (원)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PaydaySlide(payday: Int, onPaydayChange: (Int) -> Unit) {
    SlideScaffold(
        title = "수급일을 골라요",
        description = "월급·용돈 받는 날을 기준으로 한 달이 계산돼요.",
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaydayChip("15일", payday == 15) { onPaydayChange(15) }
                PaydayChip("25일", payday == 25) { onPaydayChange(25) }
                PaydayChip("말일", payday == PAYDAY_EOM) { onPaydayChange(PAYDAY_EOM) }
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
private fun PaydayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Accent else PlaceholderBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.White else Color(0xFF0D1B2A),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun PageIndicator(current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(PAGE_COUNT) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == current) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) Accent else PlaceholderBg),
            )
        }
    }
}
