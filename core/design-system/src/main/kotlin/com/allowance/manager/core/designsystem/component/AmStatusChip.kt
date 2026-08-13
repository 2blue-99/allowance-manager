package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmType

/**
 * 작은 상태 라벨 칩. 통계 증감 표시 등 짧은 텍스트를 색 배경 위에 표시.
 */
@Composable
fun AmStatusChip(
    text: String,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(AmShape.statusChip)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = AmType.size11_bold, color = color)
    }
}
