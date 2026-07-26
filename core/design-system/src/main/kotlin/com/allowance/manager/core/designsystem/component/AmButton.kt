package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape

/**
 * 기본 Primary 버튼. Material Button을 감싸 브랜드 컬러(테마 primary=에메랄드) 사용.
 * 전체폭이 필요하면 modifier에 fillMaxWidth() 전달.
 */
@Composable
fun AmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(onClick = onClick, enabled = enabled, shape = AmShape.card, modifier = modifier) {
        Text(text)
    }
}

/**
 * 보조(Outlined) 버튼. '취소' 등 Primary와 나란히 놓는 중립 액션에 사용.
 */
@Composable
fun AmOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(onClick = onClick, enabled = enabled, shape = AmShape.card, modifier = modifier) {
        Text(text)
    }
}

/**
 * 보조 텍스트 버튼(리플 없는 클릭 텍스트). '건너뛰기', '전체 보기', '수정/삭제' 등에 사용.
 */
@Composable
fun AmTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = AmColors.TextSecondary,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    )
}
