package com.allowance.manager.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType

/**
 * 선택형 칩. 선택 시 포인트 컬러(에메랄드) 배경.
 * ON/OFF 시 배경·글씨 색이 부드럽게 전환되고, 탭하면 리플이 뜬다.
 * (온보딩 예산/수급일 선택, 홈·월별 필터 칩 등 공통 사용)
 */
@Composable
fun AmChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // 기본 spring이 느긋해 빠른 tween으로 (약 120ms)
    val colorSpec = tween<Color>(durationMillis = 120)
    val bg by animateColorAsState(
        targetValue = if (selected) AmColors.Emerald else AmColors.ChipBg,
        animationSpec = colorSpec,
        label = "chipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color.White else AmColors.TextPrimary,
        animationSpec = colorSpec,
        label = "chipFg",
    )
    Box(
        modifier = modifier
            .clip(AmShape.chip)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,   // 칩은 리플 없이 색 전환만
                onClick = onClick,
            )
            .padding(horizontal = AmSpacing.lg, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = fg,
            style = AmType.value,
        )
    }
}
