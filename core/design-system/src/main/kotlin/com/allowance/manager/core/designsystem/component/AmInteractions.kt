package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color

/**
 * 리플 클릭. 리스트 아이템·칩·설정 행·바텀 네비 등 공통 사용.
 * 색은 Material 기본(테마 기준 중립색)을 쓰고, 필요 시 [rippleColor]로만 덮어쓴다.
 * 앞에 `.clip(shape)`를 두면 리플이 그 모양대로 클리핑된다.
 */
fun Modifier.amRippleClickable(
    enabled: Boolean = true,
    rippleColor: Color = Color.Unspecified,   // Unspecified = Material 기본 리플 색
    bounded: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = bounded, color = rippleColor),
        enabled = enabled,
        onClick = onClick,
    )
}
