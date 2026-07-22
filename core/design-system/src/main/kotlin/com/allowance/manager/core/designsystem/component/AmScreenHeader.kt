package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmSpacing

/**
 * 화면 상단 헤더. 뒤로가기(←, 선택) + 제목.
 * 설정/계좌 등 서브 화면과 통계 같은 탭 화면 제목에 공통 사용.
 */
@Composable
fun AmScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Text(
                text = "←",
                fontSize = 22.sp,
                color = AmColors.TextPrimary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack,
                ),
            )
            Spacer(Modifier.width(AmSpacing.md))
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AmColors.TextPrimary)
    }
}
