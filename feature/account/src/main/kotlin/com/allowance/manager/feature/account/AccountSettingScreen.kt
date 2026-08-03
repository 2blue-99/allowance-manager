package com.allowance.manager.feature.account

import androidx.compose.foundation.background
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmScreenHeader
import com.allowance.manager.core.designsystem.component.AmTextButton
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.domain.model.Account

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
            .background(AmColors.ScreenBg)
            .padding(AmSpacing.xl),
    ) {
        AmScreenHeader(title = "계좌 관리", onBack = onBack)

        Spacer(Modifier.height(AmSpacing.lg))

        if (uiState.accounts.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "아직 등록된 계좌가 없어요.\n아래에서 추가하거나,\n홈에서 감지된 거래를 메인으로 등록하세요.",
                    fontSize = 12.sp,
                    color = AmColors.TextSecondary,
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
                    Spacer(Modifier.height(AmSpacing.sm))
                }
            }
        }

        Spacer(Modifier.height(AmSpacing.sm))
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
    AmCard(modifier = Modifier.fillMaxWidth(), shape = AmShape.cardSmall) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.bankName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmColors.TextPrimary)
                Text(account.accountPattern, fontSize = 11.sp, color = AmColors.TextSecondary)
                Row {
                    AmTextButton("수정", onClick = { onStartEdit(account) }, color = AmColors.TextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.width(AmSpacing.md))
                    AmTextButton("삭제", onClick = { onDelete(account) }, color = AmColors.Red, fontSize = 11.sp)
                }
            }
            Switch(checked = account.enabled, onCheckedChange = { onToggleEnabled(account, it) })
        }
    }
}

@Composable
private fun AddAccountForm(onAdd: (String, String) -> Unit) {
    var bankName by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }

    AmCard(modifier = Modifier.fillMaxWidth(), shape = AmShape.cardSmall) {
        Column(verticalArrangement = Arrangement.spacedBy(AmSpacing.sm)) {
            Text("계좌 추가", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmColors.TextPrimary)
            AmTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = "은행/앱 이름",
                modifier = Modifier.fillMaxWidth(),
            )
            AmTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = "계좌 패턴 (예: 941602-**-***318)",
                modifier = Modifier.fillMaxWidth(),
            )
            AmButton(
                text = "추가",
                onClick = {
                    onAdd(bankName, pattern)
                    bankName = ""
                    pattern = ""
                },
                enabled = pattern.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EditAccountDialog(
    account: Account,
    onChange: (String, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AmDialog(
        title = "계좌 수정",
        onDismiss = onCancel,
        onConfirm = onSave,
        confirmEnabled = account.accountPattern.isNotBlank(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AmSpacing.sm)) {
            AmTextField(
                value = account.bankName,
                onValueChange = { onChange(it, account.accountPattern) },
                label = "은행/앱 이름",
            )
            AmTextField(
                value = account.accountPattern,
                onValueChange = { onChange(account.bankName, it) },
                label = "계좌 패턴",
            )
        }
    }
}
