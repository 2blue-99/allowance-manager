package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmType

/**
 * 밝은 화면 배경 위 아이콘 버튼 — 배경·테두리 없이 아이콘만 두고, 터치 영역(36dp)은 원형 리플로만 드러낸다.
 * 화면 상단 헤더의 보조 동작(도움말·설정 진입 등)에 쓴다. 다크 카드 위에는 쓰지 않는다.
 */
@Composable
fun AmIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .amRippleClickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 벡터 아이콘 버전 */
@Composable
fun AmIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    AmIconButton(onClick = onClick, contentDescription = contentDescription, modifier = modifier, size = size) {
        Icon(icon, contentDescription = null, tint = AmColors.NeutralBtnText, modifier = Modifier.size(22.dp))
    }
}

/** 도움말(?) 버전 — 가이드 다시 보기 등 */
@Composable
fun AmHelpIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "도움말 다시 보기",
    size: Dp = 36.dp,
) {
    AmIconButton(onClick = onClick, contentDescription = contentDescription, modifier = modifier, size = size) {
        Text("?", style = AmType.size16_bold, color = AmColors.NeutralBtnText)
    }
}
