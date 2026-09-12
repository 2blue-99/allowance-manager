package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmType

/**
 * 다크 카드 위 원형 반투명 도움말 아이콘(?) — "이 카드에 대한 도움말"을 다시 여는 재진입점.
 *
 * 금액 같은 주인공과 시선을 다투지 않게 물러난 톤(히어로 반투명 토큰). 카드 자체에 onClick이
 * 있어도 자식 clickable이 먼저 이벤트를 소비하므로 카드 탭 동작과 겹치지 않는다.
 */
@Composable
fun AmHelpIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "도움말 다시 보기",
    size: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(AmColors.HeroIconBg)
            .border(1.dp, AmColors.HeroPillLine, CircleShape)
            .amRippleClickable(rippleColor = Color.White, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text("?", style = AmType.size12_bold, color = Color.White.copy(alpha = 0.55f))
    }
}
