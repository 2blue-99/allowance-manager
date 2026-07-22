package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape

/**
 * 설정/목록 행. 흰 카드 안에 [title](+선택 [subtitle]) + 우측 [trailing] 슬롯.
 * 설정의 값 표시/네비게이션/토글, 계좌 행 등에서 공통 사용.
 */
@Composable
fun AmSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    AmCard(
        modifier = modifier.fillMaxWidth(),
        shape = AmShape.cardSmall,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmColors.TextPrimary)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 11.sp, color = AmColors.TextSecondary)
                }
            }
            trailing?.invoke(this)
        }
    }
}

/** 우측 이동 표시 셰브런(›) */
@Composable
fun AmChevron() {
    Text("›", fontSize = 20.sp, color = AmColors.TextSecondary)
}
