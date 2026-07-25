package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape

/**
 * 가로 막대형 진행바. 예산 소진율·목표 달성률 등에 공통 사용.
 * 트랙/채움 색을 파라미터로 받아 라이트 카드·다크 히어로 어디서든 쓴다.
 *
 * @param ratio 0f~1f. 범위를 벗어나면 자동으로 clamp 한다.
 */
@Composable
fun AmProgressBar(
    ratio: Float,
    modifier: Modifier = Modifier,
    fillColor: Color = AmColors.Emerald,
    trackColor: Color = AmColors.BarTrack,
    height: Dp = 8.dp,
) {
    val fraction = ratio.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(AmShape.pill)
            .background(trackColor),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(AmShape.pill)
                    .background(fillColor),
            )
        }
    }
}
