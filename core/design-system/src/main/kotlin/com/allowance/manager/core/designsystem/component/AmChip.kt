package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing

/**
 * 선택형 칩. 선택 시 포인트 컬러(에메랄드) 배경.
 * (온보딩 예산/수급일 선택, 설정 수급일 선택 등에서 공통 사용)
 */
@Composable
fun AmChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(AmShape.chip)
            .background(if (selected) AmColors.Emerald else AmColors.ChipBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = AmSpacing.lg, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else AmColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}
