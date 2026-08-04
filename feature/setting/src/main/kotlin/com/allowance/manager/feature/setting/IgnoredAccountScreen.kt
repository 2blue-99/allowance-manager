package com.allowance.manager.feature.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmScreenHeader
import com.allowance.manager.core.designsystem.component.AmTextButton
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.IgnoredAccount

@Composable
fun IgnoredAccountRoute(
    onBack: () -> Unit,
    viewModel: IgnoredAccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    IgnoredAccountScreen(
        uiState = uiState,
        onBack = onBack,
        onRemove = viewModel::onRemove,
    )
}

@Composable
fun IgnoredAccountScreen(
    uiState: IgnoredAccountUiState,
    onBack: () -> Unit = {},
    onRemove: (Long) -> Unit = {},
) {
    // 삭제(무시 해제) 확인 대상 (null = 닫힘). 순수 UI 상태라 화면이 보유.
    var pendingRemove by remember { mutableStateOf<IgnoredAccount?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmColors.ScreenBg)
            .padding(AmSpacing.xl),
    ) {
        AmScreenHeader(title = "무시 계좌 관리", onBack = onBack)
        Spacer(Modifier.height(AmSpacing.lg))

        if (uiState.accounts.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "무시한 알림이 없어요.\n홈에서 내역을 눌러 무시할 수 있어요.",
                    fontSize = 12.sp,
                    color = AmColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.accounts, key = { it.id }) { account ->
                    IgnoredAccountRow(account = account, onRemove = { pendingRemove = account })
                    Spacer(Modifier.height(AmSpacing.sm))
                }
            }
        }
    }

    pendingRemove?.let { account ->
        AmDialog(
            title = "무시를 해제할까요?",
            onDismiss = { pendingRemove = null },
            onConfirm = { onRemove(account.id); pendingRemove = null },
            confirmText = "해제",
            confirmColor = AmColors.Red,
            analyticsTag = "ignore_remove",
        ) {
            Text(
                "${account.sourceName}에서 오는 알림을 다시 받아요. 이미 삭제된 과거 내역은 복구되지 않아요.",
                style = AmType.body,
                color = AmColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun IgnoredAccountRow(
    account: IgnoredAccount,
    onRemove: () -> Unit,
) {
    AmCard(modifier = Modifier.fillMaxWidth(), shape = AmShape.cardSmall) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.sourceName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmColors.TextPrimary)
                Text(
                    // 계좌번호 있으면 그 번호만, 없으면 앱 전체
                    if (account.isNumberBased) account.accountPattern else "이 앱의 모든 알림",
                    fontSize = 11.sp,
                    color = AmColors.TextSecondary,
                )
            }
            AmTextButton("해제", onClick = onRemove, color = AmColors.Red, fontSize = 13.sp)
        }
    }
}
