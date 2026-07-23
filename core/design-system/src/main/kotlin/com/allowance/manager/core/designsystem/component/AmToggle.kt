package com.allowance.manager.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.allowance.manager.core.designsystem.theme.AmColors

/**
 * 작은 토글 스위치. Material Switch는 목록 헤더에 쓰기엔 커서 별도로 둔다.
 * 켜짐이면 포인트 컬러(에메랄드), 꺼짐이면 회색 트랙.
 */
@Composable
fun AmToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 34.dp,
    trackHeight: Dp = 20.dp,
) {
    val gap = 2.dp
    val thumbSize = trackHeight - gap * 2

    val trackColor by animateColorAsState(
        targetValue = if (checked) AmColors.Emerald else AmColors.BarTrack,
        animationSpec = tween(durationMillis = 180),
        label = "amToggleTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - gap else gap,
        animationSpec = tween(durationMillis = 180),
        label = "amToggleThumb",
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .align(Alignment.CenterStart)
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
