package com.allowance.manager.feature.account

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.domain.model.Account

private val Bg = Color(0xFFF0F2F6)
private val TextPrimary = Color(0xFF0D1B2A)
private val TextSecondary = Color(0xFF8A97AA)
private val SpendRed = Color(0xFFEF4444)

@Composable
fun AccountSettingRoute(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AccountSettingScreen(
        uiState = uiState,
        onBack = onBack,
        onAdd = viewModel::onAdd,
        onToggleEnabled = viewModel::onToggleEnabled,
        onDelete = viewModel::onDelete,
        onStartEdit = viewModel::onStartEdit,
        onEditChange = viewModel::onEditChange,
        onSaveEdit = viewModel::onSaveEdit,
        onCancelEdit = viewModel::onCancelEdit,
    )
}

@Composable
fun AccountSettingScreen(
    uiState: AccountUiState,
    onBack: () -> Unit = {},
    onAdd: (String, String) -> Unit = { _, _ -> },
    onToggleEnabled: (Account, Boolean) -> Unit = { _, _ -> },
    onDelete: (Account) -> Unit = {},
    onStartEdit: (Account) -> Unit = {},
    onEditChange: (String, String) -> Unit = { _, _ -> },
    onSaveEdit: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("←", fontSize = 22.sp, color = TextPrimary, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text("계좌 관리", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Spacer(Modifier.height(16.dp))

        if (uiState.accounts.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "아직 등록된 계좌가 없어요.\n아래에서 추가하거나,\n홈에서 감지된 거래를 메인으로 등록하세요.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.accounts, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        onToggleEnabled = onToggleEnabled,
                        onDelete = onDelete,
                        onStartEdit = onStartEdit,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        AddAccountForm(onAdd = onAdd)
    }

    uiState.editing?.let { editing ->
        EditAccountDialog(
            account = editing,
            onChange = onEditChange,
            onSave = onSaveEdit,
            onCancel = onCancelEdit,
        )
    }
}

@Composable
private fun AccountRow(
    account: Account,
    onToggleEnabled: (Account, Boolean) -> Unit,
    onDelete: (Account) -> Unit,
    onStartEdit: (Account) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(account.bankName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(account.accountPattern, fontSize = 11.sp, color = TextSecondary)
            Row {
                Text("수정", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.clickable { onStartEdit(account) })
                Spacer(Modifier.width(12.dp))
                Text("삭제", fontSize = 11.sp, color = SpendRed, modifier = Modifier.clickable { onDelete(account) })
            }
        }
        Switch(checked = account.enabled, onCheckedChange = { onToggleEnabled(account, it) })
    }
}

@Composable
private fun AddAccountForm(onAdd: (String, String) -> Unit) {
    var bankName by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("계좌 추가", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        OutlinedTextField(
            value = bankName,
            onValueChange = { bankName = it },
            label = { Text("은행/앱 이름") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = pattern,
            onValueChange = { pattern = it },
            label = { Text("계좌 패턴 (예: 941602-**-***318)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                onAdd(bankName, pattern)
                bankName = ""
                pattern = ""
            },
            enabled = pattern.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("추가") }
    }
}

@Composable
private fun EditAccountDialog(
    account: Account,
    onChange: (String, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("계좌 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = account.bankName,
                    onValueChange = { onChange(it, account.accountPattern) },
                    label = { Text("은행/앱 이름") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = account.accountPattern,
                    onValueChange = { onChange(account.bankName, it) },
                    label = { Text("계좌 패턴") },
                    singleLine = true,
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = account.accountPattern.isNotBlank()) { Text("저장") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("취소") } },
    )
}
