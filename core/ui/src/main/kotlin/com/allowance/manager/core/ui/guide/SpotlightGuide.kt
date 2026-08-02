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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.geometry.CornerRadius
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

/** 스포트라이트 구멍 모양 — 카드·내역은 요소를 감싸는 둥근 사각형, 버튼(추가)은 원 */
enum class SpotShape { RECT, CIRCLE }

/** 가이드 한 스텝: 강조 대상 key + 문구 + 모양 */
data class GuideStep(
    val key: String,
    val message: String,
    val shape: SpotShape = SpotShape.RECT,
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

private val DimColor = Color(0xFF0F1728).copy(alpha = 0.68f)

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
    val rectWindow = targets[step.key] ?: return
    val isLast = index == visible.lastIndex

    fun next() { if (isLast) onFinish() else rawIndex = index + 1 }
    fun prev() { if (index > 0) rawIndex = index - 1 }

    // ⚠️ 상태바 높이는 반드시 팝업 '밖'(메인 창)에서 읽는다.
    // 팝업은 별도 창이라 그 안에서 WindowInsets를 읽으면 0으로 잡혀 보정이 무효가 됨.
    // 대상 좌표(boundsInWindow)는 상태바 포함 메인 창 기준인데, 팝업 오버레이는 상태바 아래에서 시작 →
    // 그 차이(상태바 높이)만큼 위로 보정해야 구멍이 실제 요소와 정렬된다.
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.getTop(density).toFloat()

    Popup(
        popupPositionProvider = fullscreenPositionProvider,
        properties = PopupProperties(focusable = true),
        onDismissRequest = onFinish,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenH = constraints.maxHeight.toFloat()

            val rect = rectWindow.translate(0f, -statusBarTop)

            // 구멍이 요소를 감싸는 여백 + 둥근 모서리 반경 — Canvas·문구 위치에서 공유
            val pad = with(density) { 6.dp.toPx() }
            val cornerPx = with(density) { 16.dp.toPx() }

            // ── 반어둠 + 구멍 ──
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
            ) {
                drawRect(DimColor)
                when (step.shape) {
                    SpotShape.CIRCLE -> {
                        val r = maxOf(rect.width, rect.height) / 2f * 1.35f + pad
                        drawCircle(Color.Transparent, r, rect.center, blendMode = BlendMode.Clear)
                    }
                    SpotShape.RECT -> {
                        // 요소를 딱 감싸는 둥근 사각형 (카드·내역 모양과 일치)
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset(rect.left - pad, rect.top - pad),
                            size = Size(rect.width + pad * 2, rect.height + pad * 2),
                            cornerRadius = CornerRadius(cornerPx, cornerPx),
                            blendMode = BlendMode.Clear,
                        )
                    }
                }
            }

            // ── 문구 + 버튼 (구멍 가장자리에서 띄워 겹치지 않게) ──
            var tipHeight by remember(index) { mutableIntStateOf(0) }
            // 구멍(타원/원)의 실제 세로 반지름 → rect가 아니라 구멍 가장자리 기준으로 배치
            val holeHalfH = when (step.shape) {
                SpotShape.CIRCLE -> maxOf(rect.width, rect.height) / 2f * 1.35f + pad
                SpotShape.RECT -> rect.height / 2f + pad
            }
            val holeTop = rect.center.y - holeHalfH
            val holeBottom = rect.center.y + holeHalfH
            val gapPx = with(density) { 24.dp.toPx() }   // 구멍과 문구 사이 간격
            val sidePx = with(density) { 22.dp.toPx() }
            val below = rect.center.y < screenH * 0.5f
            val tipY = if (below) holeBottom + gapPx else (holeTop - gapPx - tipHeight)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 뒤로가기 (첫 스텝에선 숨김)
                    if (index > 0) {
                        Text(
                            "‹ 뒤로",
                            color = Color.White.copy(alpha = 0.62f),
                            fontSize = 13.sp,
                            modifier = Modifier
                                .noRippleClickable { prev() }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                    // 다음 / 시작하기
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
                    // 건너뛰기 — 마지막 스텝에선 숨김('시작하기'만 노출)
                    if (!isLast) {
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "건너뛰기",
                            color = Color.White.copy(alpha = 0.62f),
                            fontSize = 13.sp,
                            modifier = Modifier
                                .noRippleClickable { onFinish() }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                        )
                    }
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
