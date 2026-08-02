package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing

/**
 * 앱 공통 카드 컨테이너. 둥근 모서리 + 배경 + 패딩을 표준화.
 * 내용 레이아웃(Row/Column)은 호출부에서 content 슬롯에 넣는다.
 */
@Composable
fun AmCard(
    modifier: Modifier = Modifier,
    color: Color = AmColors.CardBg,
    shape: Shape = AmShape.card,
    contentPadding: PaddingValues = PaddingValues(AmSpacing.lg),
    onClick: (() -> Unit)? = null,
    rippleColor: Color = Color.Unspecified,   // 어두운 카드 등에서 리플색을 덮어쓸 때
    content: @Composable () -> Unit,
) {
    val shaped = modifier
        .clip(shape)
        .background(color)
        .let { if (onClick != null) it.amRippleClickable(rippleColor = rippleColor, onClick = onClick) else it }
        .padding(contentPadding)
    Box(modifier = shaped) { content() }
}
