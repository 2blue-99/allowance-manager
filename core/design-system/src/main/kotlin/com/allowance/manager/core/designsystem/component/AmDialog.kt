package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType

/**
 * 앱 공통 다이얼로그. 제목 + 자유 [content] 슬롯 + 확인/취소 버튼을 표준화.
 * Material AlertDialog 대신 이걸 써서 브랜드 색·모서리·타이포를 일관되게 유지한다.
 *
 * @param confirmEnabled false면 확인 버튼이 흐려지고 눌러도 동작하지 않는다.
 */
@Composable
fun AmDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "저장",
    dismissText: String = "취소",
    confirmEnabled: Boolean = true,
    confirmColor: Color = AmColors.Emerald,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(AmShape.cardLarge)
                .background(AmColors.CardBg)
                .padding(AmSpacing.xl),
        ) {
            Text(title, style = AmType.dialogTitle, color = AmColors.TextPrimary)
            Spacer(Modifier.height(AmSpacing.lg))
            content()
            Spacer(Modifier.height(AmSpacing.xl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AmSpacing.xl, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // dismissText가 비면 취소 버튼 숨김 (단일 버튼 다이얼로그 지원)
                if (dismissText.isNotBlank()) {
                    AmTextButton(text = dismissText, onClick = onDismiss, color = AmColors.TextSecondary)
                }
                AmTextButton(
                    text = confirmText,
                    onClick = { if (confirmEnabled) onConfirm() },
                    color = if (confirmEnabled) confirmColor else AmColors.TextTertiary,
                )
            }
        }
    }
}
