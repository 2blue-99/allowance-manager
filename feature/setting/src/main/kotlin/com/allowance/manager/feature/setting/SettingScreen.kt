package com.allowance.manager.feature.setting

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.designsystem.theme.AmColors

private val Bg = AmColors.ScreenBg
private val Accent = AmColors.Emerald
private val PlaceholderBg = AmColors.ChipBg
private val TextPrimary = AmColors.TextPrimary
private val TextSecondary = AmColors.TextSecondary

private const val PAYDAY_EOM = 0

private fun paydayLabel(payday: Int): String = if (payday <= 0) "말일" else "${payday}일"

@Composable
fun SettingScreen(
    onBack: () -> Unit,
    onNavigateToAccount: () -> Unit = {},
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showPaydayDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Bg).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("←", fontSize = 22.sp, color = TextPrimary, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text("설정", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Spacer(Modifier.height(20.dp))

        ValueItem(title = "월 예산", value = "${uiState.budget.amountToComma()}원") { showBudgetDialog = true }
        Spacer(Modifier.height(8.dp))
        ValueItem(title = "수급일", value = paydayLabel(uiState.payday)) { showPaydayDialog = true }
        Spacer(Modifier.height(8.dp))
        ToggleItem(
            title = "상태바 알림",
            subtitle = "이번달 지출을 상태바에 상시 표시",
            checked = uiState.statusBarEnabled,
            onCheckedChange = viewModel::setStatusBarEnabled,
        )
        Spacer(Modifier.height(8.dp))
        NavItem(title = "계좌 관리", subtitle = "메인 계좌 등록·수정", onClick = onNavigateToAccount)
    }

    if (showBudgetDialog) {
        BudgetDialog(
            current = uiState.budget,
            onSave = { viewModel.setBudget(it); showBudgetDialog = false },
            onDismiss = { showBudgetDialog = false },
        )
    }
    if (showPaydayDialog) {
        PaydayDialog(
            current = uiState.payday,
            onSave = { viewModel.setPayday(it); showPaydayDialog = false },
            onDismiss = { showPaydayDialog = false },
        )
    }
}

@Composable
private fun ValueItem(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Accent)
        Spacer(Modifier.width(6.dp))
        Text("›", fontSize = 20.sp, color = TextSecondary)
    }
}

@Composable
private fun NavItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Text("›", fontSize = 20.sp, color = TextSecondary)
    }
}

@Composable
private fun ToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BudgetDialog(current: Long, onSave: (Long) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("월 예산") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { v -> input = v.filter { it.isDigit() } },
                label = { Text("월 예산 (원)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
        title = { Text("수급일") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaydayChip("15일", payday == 15) { payday = 15 }
                    PaydayChip("25일", payday == 25) { payday = 25 }
                    PaydayChip("말일", payday == PAYDAY_EOM) { payday = PAYDAY_EOM }
                }
                OutlinedTextField(
                    value = if (payday in 1..31) payday.toString() else "",
                    onValueChange = { v ->
                        val day = v.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 31)
                        if (day != null) payday = day
                    },
                    label = { Text("직접 입력 (1~31)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(payday) }) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun PaydayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Accent else PlaceholderBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.White else TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}
