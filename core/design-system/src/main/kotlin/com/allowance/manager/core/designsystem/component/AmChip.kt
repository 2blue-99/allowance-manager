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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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
    // 선택 시 채움색 — 기본은 브랜드 에메랄드. 카테고리 칩은 각 카테고리색을 넘긴다.
    selectedColor: Color = AmColors.Emerald,
    // 라벨 스타일 — 기본은 공통 칩 크기. 더 큰 칩(유형 선택 등)에서 오버라이드.
    textStyle: TextStyle = AmType.size13_bold,
    onClick: () -> Unit,
) {
    // 기본 spring이 느긋해 빠른 tween으로 (약 120ms)
    val colorSpec = tween<Color>(durationMillis = 120)
    val bg by animateColorAsState(
        targetValue = if (selected) selectedColor else AmColors.ChipBg,
        animationSpec = colorSpec,
        label = "chipBg",
    )
    val fg by animateColorAsState(
        // 미선택 라벨은 진한 네이비보다 한 톤 낮춘 회색으로 (덜 무겁게)
        targetValue = if (selected) Color.White else AmColors.NeutralBtnText,
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
        // 내용폭 칩은 무영향, full-width(weight) 칩에선 라벨이 가운데 정렬됨
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = fg,
            style = textStyle,
        )
    }
}
