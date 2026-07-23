package com.allowance.manager.feature.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmChevron
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.AmScreenHeader
import com.allowance.manager.core.designsystem.component.AmSettingRow
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.domain.util.amountToComma

private const val PAYDAY_EOM = 0

private fun paydayLabel(payday: Int): String = if (payday <= 0) "말일" else "${payday}일"

/** 상태 계층: ViewModel·네비게이션 의존성을 여기서 관리한다. */
@Composable
fun SettingRoute(
    onBack: () -> Unit,
    onNavigateToAccount: () -> Unit = {},
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingScreen(
        uiState = uiState,
        onBack = onBack,
        onNavigateToAccount = onNavigateToAccount,
        onStatusBarEnabledChange = viewModel::setStatusBarEnabled,
        onBudgetChange = viewModel::setBudget,
        onPaydayChange = viewModel::setPayday,
    )
}

/** 표현 계층: uiState와 콜백만 받는다. (Preview 가능) */
@Composable
fun SettingScreen(
    uiState: SettingUiState,
    onBack: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onStatusBarEnabledChange: (Boolean) -> Unit = {},
    onBudgetChange: (Long) -> Unit = {},
    onPaydayChange: (Int) -> Unit = {},
) {
    // 다이얼로그 노출 여부는 순수 UI 상태 → 화면이 직접 보유
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showPaydayDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(AmColors.ScreenBg).padding(AmSpacing.xl),
    ) {
        AmScreenHeader(title = "설정", onBack = onBack)

        Spacer(Modifier.height(AmSpacing.xl))

        AmSettingRow(title = "월 예산", onClick = { showBudgetDialog = true }) {
            Text("${uiState.budget.amountToComma()}원", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmColors.Emerald)
            Spacer(Modifier.width(AmSpacing.xs))
            AmChevron()
        }
        Spacer(Modifier.height(AmSpacing.sm))
        AmSettingRow(title = "월급일", onClick = { showPaydayDialog = true }) {
            Text(paydayLabel(uiState.payday), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmColors.Emerald)
            Spacer(Modifier.width(AmSpacing.xs))
            AmChevron()
        }
        Spacer(Modifier.height(AmSpacing.sm))
        AmSettingRow(title = "상태바 알림", subtitle = "이번달 지출을 상태바에 상시 표시") {
            Switch(checked = uiState.statusBarEnabled, onCheckedChange = onStatusBarEnabledChange)
        }
        Spacer(Modifier.height(AmSpacing.sm))
        AmSettingRow(title = "계좌 관리", subtitle = "메인 계좌 등록·수정", onClick = onNavigateToAccount) {
            AmChevron()
        }
    }

    if (showBudgetDialog) {
        BudgetDialog(
            current = uiState.budget,
            onSave = { onBudgetChange(it); showBudgetDialog = false },
            onDismiss = { showBudgetDialog = false },
        )
    }
    if (showPaydayDialog) {
        PaydayDialog(
            current = uiState.payday,
            onSave = { onPaydayChange(it); showPaydayDialog = false },
            onDismiss = { showPaydayDialog = false },
        )
    }
}

@Composable
private fun BudgetDialog(current: Long, onSave: (Long) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("월 예산") },
        text = {
            AmTextField(
                value = input,
                onValueChange = { v -> input = v.filter { it.isDigit() } },
                label = "월 예산 (원)",
                keyboardType = KeyboardType.Number,
            )
        },
        confirmButton = {
            val amount = input.toLongOrNull() ?: 0L
            TextButton(onClick = { onSave(amount) }, enabled = amount > 0) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun PaydayDialog(current: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var payday by remember { mutableIntStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("월급일") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AmSpacing.md)) {
                Row(horizontalArrangement = Arrangement.spacedBy(AmSpacing.sm)) {
                    AmChip("15일", payday == 15) { payday = 15 }
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
            }
        },
        confirmButton = { TextButton(onClick = { onSave(payday) }) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
