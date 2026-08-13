package com.allowance.manager.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape

/** 버튼 공통 기본 높이 */
val AmButtonHeight: Dp = 52.dp

/**
 * 기본 Primary 버튼. Material Button을 감싸 브랜드 컬러(테마 primary=에메랄드) 사용.
 * 전체폭이 필요하면 modifier에 fillMaxWidth() 전달.
 *
 * @param containerColor 채움색을 덮어쓸 때(예: 삭제=빨강). null이면 테마 primary(에메랄드).
 */
@Composable
fun AmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = AmButtonHeight,
    containerColor: Color? = null,
    // null이면 Material 기본 라벨 스타일. 더 두꺼운 라벨이 필요하면 AmType 토큰을 넘긴다.
    textStyle: TextStyle? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "amButtonScale")
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = AmShape.card,
        colors = if (containerColor != null) {
            ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = Color.White)
        } else {
            ButtonDefaults.buttonColors()
        },
        interactionSource = interaction,
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        if (textStyle != null) Text(text, style = textStyle) else Text(text)
    }
}

/**
 * 보조(회색 채움) 버튼. '취소' 등 Primary와 나란히 놓는 중립 액션에 사용(토스식 채움 톤).
 * Primary와 같은 '채움' 언어라 한 세트로 자연스럽게 보인다.
 */
@Composable
fun AmSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = AmButtonHeight,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "amSecondaryButtonScale")
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = AmShape.card,
        colors = ButtonDefaults.buttonColors(
            containerColor = AmColors.NeutralBtnBg,
            contentColor = AmColors.NeutralBtnText,
        ),
        interactionSource = interaction,
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
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
    color: Color = AmColors.TextSecondary,
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
