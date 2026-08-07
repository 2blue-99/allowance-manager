package com.allowance.manager.core.ui.transaction

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 왼쪽으로 밀면 숨김·삭제 액션을 드러내는 행 (draggable + Animatable, 안정 API).
 * 홈·월별 등 내역 리스트에서 공통으로 사용한다.
 */
@Composable
fun SwipeRevealRow(
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidth = 68.dp
    val maxReveal = with(density) { (actionWidth * 2).toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.clip(AmShape.card)) {
        // 배경: 오른쪽에 제외·삭제
        Row(modifier = Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
            SwipeAction(
                icon = if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                label = if (hidden) "복원" else "제외",
                background = AmColors.ChipBg,
                foreground = AmColors.TextSecondary,
                width = actionWidth,
            ) { scope.launch { offsetX.animateTo(0f) }; onToggleHidden() }
            SwipeAction(
                icon = Icons.Outlined.Delete,
                label = "삭제",
                background = AmColors.Red,
                foreground = Color.White,
                width = actionWidth,
            ) { scope.launch { offsetX.animateTo(0f) }; onDelete() }
        }
        // 전경: 실제 카드 (드래그로 좌측 이동)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(-maxReveal, 0f)) }
                    },
                    onDragStopped = {
                        val target = if (offsetX.value < -maxReveal / 2f) -maxReveal else 0f
                        offsetX.animateTo(target)
                    },
                ),
        ) { content() }
    }
}

@Composable
private fun SwipeAction(
    icon: ImageVector,
    label: String,
    background: Color,
    foreground: Color,
    width: Dp,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = foreground, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = AmType.tiny, color = foreground)
    }
}
