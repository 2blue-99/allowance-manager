package com.allowance.manager.core.ui.guide

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.allowance.manager.core.designsystem.theme.AmColors
import kotlin.math.roundToInt

/** 스포트라이트 구멍 모양 — 카드·내역은 타원, 버튼(추가·설정)은 원 */
enum class SpotShape { OVAL, CIRCLE }

/** 가이드 한 스텝: 강조 대상 key + 문구 + 모양 */
data class GuideStep(
    val key: String,
    val message: String,
    val shape: SpotShape = SpotShape.OVAL,
)

/** 강조 대상들의 창(window) 좌표 저장소 */
@Composable
fun rememberGuideTargets(): SnapshotStateMap<String, Rect> = remember { mutableStateMapOf() }

/** 강조 대상 컴포저블에 붙여 위치를 등록 */
fun Modifier.guideTarget(key: String, targets: SnapshotStateMap<String, Rect>): Modifier =
    this.onGloballyPositioned { targets[key] = it.boundsInWindow() }

// 창 전체(0,0)에 오버레이 → 바텀 네비까지 덮음
private val fullscreenPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

private val DimColor = Color(0xFF0F1728).copy(alpha = 0.62f)

/**
 * 홈 최초 진입 가이드(스포트라이트). 대상 좌표가 잡힌 스텝만 노출한다(내역 없으면 자동 스킵).
 * 배경을 반어둡게 덮고 대상 자리에 타원/원 구멍을 뚫으며, 구멍 근처에 문구+버튼(V3)을 띄운다.
 */
@Composable
fun SpotlightGuide(
    steps: List<GuideStep>,
    targets: SnapshotStateMap<String, Rect>,
    onFinish: () -> Unit,
) {
    // 좌표가 측정된 스텝만 (측정 전이거나 없는 대상은 제외)
    val visible = steps.filter { targets[it.key] != null }
    if (visible.isEmpty()) return

    var rawIndex by remember { mutableIntStateOf(0) }
    val index = rawIndex.coerceIn(0, visible.lastIndex)
    val step = visible[index]
    val rect = targets[step.key] ?: return
    val isLast = index == visible.lastIndex

    fun next() { if (isLast) onFinish() else rawIndex = index + 1 }

    Popup(
        popupPositionProvider = fullscreenPositionProvider,
        properties = PopupProperties(focusable = true),
        onDismissRequest = onFinish,
    ) {
        val density = LocalDensity.current
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenH = constraints.maxHeight.toFloat()

            // ── 반어둠 + 구멍 ──
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
            ) {
                drawRect(DimColor)
                val pad = with(density) { 8.dp.toPx() }
                when (step.shape) {
                    SpotShape.CIRCLE -> {
                        val r = maxOf(rect.width, rect.height) / 2f * 1.35f + pad
                        drawCircle(Color.Transparent, r, rect.center, blendMode = BlendMode.Clear)
                    }
                    SpotShape.OVAL -> {
                        val ow = rect.width * 1.42f + pad * 2
                        val oh = rect.height * 1.42f + pad * 2
                        drawOval(
                            color = Color.Transparent,
                            topLeft = Offset(rect.center.x - ow / 2f, rect.center.y - oh / 2f),
                            size = Size(ow, oh),
                            blendMode = BlendMode.Clear,
                        )
                    }
                }
            }

            // ── 문구 + 버튼 (구멍 위/아래 여유 있는 쪽) ──
            var tipHeight by remember(index) { mutableIntStateOf(0) }
            val marginPx = with(density) { 20.dp.toPx() }
            val sidePx = with(density) { 22.dp.toPx() }
            val below = rect.center.y < screenH * 0.5f
            val tipY = if (below) rect.bottom + marginPx else (rect.top - marginPx - tipHeight)
                .coerceAtLeast(with(density) { 24.dp.toPx() })

            Column(
                modifier = Modifier
                    .offset { IntOffset(sidePx.roundToInt(), tipY.roundToInt()) }
                    .width(260.dp)
                    .onGloballyPositioned { tipHeight = it.size.height },
            ) {
                Text(
                    "${index + 1} / ${visible.size}",
                    color = Color(0xFF5EE0A8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.padding(top = 6.dp))
                Text(
                    step.message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.padding(top = 14.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        "건너뛰기",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .noRippleClickable { onFinish() }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        if (isLast) "시작하기" else "다음 ›",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AmColors.Emerald)
                            .noRippleClickable { next() }
                            .padding(horizontal = 20.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}

private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}
