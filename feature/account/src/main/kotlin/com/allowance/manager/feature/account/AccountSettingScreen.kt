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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmScreenHeader
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.Account
import kotlinx.coroutines.withTimeoutOrNull

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
    )
}

@Composable
fun AccountSettingScreen(
    uiState: AccountUiState,
    onBack: () -> Unit = {},
    onAdd: (String, String) -> Unit = { _, _ -> },
    onToggleEnabled: (Account, Boolean) -> Unit = { _, _ -> },
    onDelete: (Account) -> Unit = {},
) {
    // 삭제 확인 대상 (null = 닫힘). 순수 UI 상태라 화면이 보유.
    var pendingDelete by remember { mutableStateOf<Account?>(null) }
    // 토글 결과 알림 — 홈과 동일한 완료 피드백(스낵바 + 햅틱) 패턴
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingMessage by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pendingMessage) {
        val msg = pendingMessage ?: return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        withTimeoutOrNull(1500) { snackbarHostState.showSnackbar(msg) }
        pendingMessage = null
    }

    Box(modifier = Modifier.fillMaxSize().background(AmColors.ScreenBg)) {
        Column(modifier = Modifier.fillMaxSize().padding(AmSpacing.xl)) {
            AmScreenHeader(title = "계좌 관리", onBack = onBack)

            Spacer(Modifier.height(AmSpacing.lg))

            // 입력창을 상단에 고정 — 키보드가 하단에서 올라와도 가려지지 않도록
            AddAccountForm(onAdd = onAdd)

            Spacer(Modifier.height(AmSpacing.lg))

            if (uiState.accounts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "아직 등록된 계좌가 없어요.\n위에서 추가하거나,\n홈에서 감지된 거래를 메인으로 등록하세요.",
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
                            onToggleEnabled = { acc, enabled ->
                                onToggleEnabled(acc, enabled)
                                pendingMessage =
                                    if (enabled) "메인 계좌에 등록되었습니다" else "메인 계좌에서 해제되었습니다"
                            },
                            onDelete = { pendingDelete = account },
                        )
                        Spacer(Modifier.height(AmSpacing.sm))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = AmSpacing.lg),
        )
    }

    // 삭제는 되돌릴 수 없으므로 확인 후 진행 (홈·월별 삭제 팝업과 동일 패턴)
    pendingDelete?.let { account ->
        AmDialog(
            title = "계좌를 삭제할까요?",
            onDismiss = { pendingDelete = null },
            onConfirm = { onDelete(account); pendingDelete = null },
            confirmText = "삭제",
            confirmColor = AmColors.Red,
        ) {
            Text("삭제하면 되돌릴 수 없어요.", style = AmType.body, color = AmColors.TextSecondary)
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    onToggleEnabled: (Account, Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    AmCard(modifier = Modifier.fillMaxWidth(), shape = AmShape.cardSmall) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.bankName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmColors.TextPrimary)
                Text(account.accountPattern, fontSize = 11.sp, color = AmColors.TextSecondary)
            }
            // 삭제: 내역 상세와 동일한 휴지통 아이콘. 탭하면 삭제 확인 팝업.
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "삭제",
                    tint = AmColors.Red,
                    modifier = Modifier.size(20.dp),
                )
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
                label = "계좌 패턴",
                supportingText = "ex. 941111-11-111111 (\"-\" 제외)",
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
