package com.allowance.manager.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape

/**
 * iOS식 세그먼트 토글 — 회색 트랙 위로 흰 pill이 슬라이드한다.
 * 유형 필터(전체/지출/수입) 등 상호배타 단일 선택에 사용.
 */
@Composable
fun AmSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pad = 3.dp
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(AmShape.card)
            .background(AmColors.NeutralBtnBg)
            .padding(pad),
    ) {
        // maxWidth 는 이미 padding 이 빠진 내부 폭 → 그대로 n등분 (pad 재차감 금지)
        val segWidth = maxWidth / options.size
        val thumbOffset by animateDpAsState(
            targetValue = segWidth * selectedIndex.coerceIn(0, options.size - 1),
            animationSpec = tween(durationMillis = 220),
            label = "segThumb",
        )
        // 슬라이드하는 흰 pill (텍스트 아래에 깔림)
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segWidth)
                .fillMaxHeight()
                .clip(AmShape.chip)
                .background(AmColors.CardBg),
        )
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val on = i == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(i) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        // 선택 = 앱 메인색(에메랄드), 미선택 = 보조 텍스트
                        color = if (on) AmColors.Emerald else AmColors.TextSecondary,
                    )
                }
            }
        }
    }
}
