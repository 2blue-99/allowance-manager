package com.allowance.manager.core.ui.guide

import android.content.Context
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.LocalAnalyticsHelper
import com.allowance.manager.core.designsystem.theme.AmColors
import kotlin.math.roundToInt

/** 스포트라이트 구멍 모양 — 카드·내역은 요소를 감싸는 둥근 사각형, 버튼(추가)은 원 */
enum class SpotShape { RECT, CIRCLE }

/**
 * 가이드 한 스텝: 강조 대상 key(들) + 헤드라인/설명 + 모양.
 * - keys가 여러 개면 그 영역들을 감싸는 union을 하나의 구멍으로 하이라이트한다(예: 월별+통계 탭).
 * - keys가 비어 있으면(anchorless) 구멍 없이 화면 전체를 덮고, [image]·[action]으로 홍보성 스텝을 구성한다(예: 위젯 안내).
 */
data class GuideStep(
    val keys: List<String>,
    val title: String,
    val message: String,
    val shape: SpotShape = SpotShape.RECT,
    @DrawableRes val image: Int? = null,
    val action: GuideAction? = null,
    // [message] 안에서 강조(에메랄드+굵게)할 부분 문자열. null이면 강조 없음.
    val emphasis: String? = null,
) {
    constructor(key: String, title: String, message: String, shape: SpotShape = SpotShape.RECT) :
        this(listOf(key), title, message, shape)
}

/**
 * 앵커 없는 스텝의 커스텀 버튼. 기본 '다음/시작하기' 대신 [label] 버튼을 노출한다.
 * 누르면 [onClick] 실행 후 가이드가 종료된다. [secondaryLabel]은 옆의 보조 텍스트(없으면 미노출).
 */
data class GuideAction(
    val label: String,
    val onClick: () -> Unit,
    val secondaryLabel: String? = null,
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
private val CounterColor = Color(0xFF5EE0A8)
private val TipDescColor = Color(0xFFD6DEEA)
private val EmphasisColor = Color(0xFF5EE0A8) // 설명 문구 강조(딤 배경에서 잘 읽히는 에메랄드)

/**
 * 홈 최초 진입 가이드(스포트라이트). 대상 좌표가 잡힌 스텝만 노출한다(내역 없으면 자동 스킵).
 * 배경을 반어둡게 덮고 대상 자리에 둥근 사각형/원 구멍을 뚫으며, 구멍 근처에 헤드라인+설명+버튼을 띄운다.
 * anchorless 스텝(keys 비어 있음)은 구멍 없이 전체를 덮고 이미지·설명·액션 버튼을 중앙에 배치한다.
 */
@Composable
fun SpotlightGuide(
    steps: List<GuideStep>,
    targets: SnapshotStateMap<String, Rect>,
    onFinish: () -> Unit,
) {
    // 좌표가 측정된 스텝만 (측정 전이거나 없는 대상은 제외). anchorless(keys 비어 있음)는 항상 통과.
    val visible = steps.filter { s -> s.keys.all { targets[it] != null } }
    if (visible.isEmpty()) return

    val analytics = LocalAnalyticsHelper.current
    var rawIndex by remember { mutableIntStateOf(0) }
    val index = rawIndex.coerceIn(0, visible.lastIndex)
    val step = visible[index]
    val isLast = index == visible.lastIndex

    // 등장: 페이드 인 / 종료(건너뛰기·시작하기·뒤로가기 밖 탭): 페이드 아웃 후 onFinish
    var finishing by remember { mutableStateOf(false) }
    val overlayAlpha = remember { Animatable(0f) }
    LaunchedEffect(finishing) {
        if (finishing) {
            overlayAlpha.animateTo(0f, tween(durationMillis = 180))
            onFinish()
        } else {
            overlayAlpha.animateTo(1f, tween(durationMillis = 260))
        }
    }
    // 종료: 마지막 스텝까지 봤으면 완료, 아니면 중간 이탈(건너뛰기). step은 1-based 현재 스텝.
    fun finish() {
        if (finishing) return
        if (isLast) {
            analytics.logEvent(AmAnalytics.Event.GUIDE_COMPLETE)
        } else {
            analytics.logEvent(AmAnalytics.Event.GUIDE_SKIP, mapOf(AmAnalytics.Param.STEP to index + 1))
        }
        finishing = true
    }
    fun next() {
        if (isLast) {
            finish()
        } else {
            analytics.logEvent(AmAnalytics.Event.GUIDE_SLIDE, mapOf(AmAnalytics.Param.STEP to index + 1))
            rawIndex = index + 1
        }
    }
    fun prev() {
        if (index > 0) {
            analytics.logEvent(AmAnalytics.Event.GUIDE_BACK, mapOf(AmAnalytics.Param.STEP to index + 1))
            rawIndex = index - 1
        }
    }

    val density = LocalDensity.current

    Popup(
        popupPositionProvider = fullscreenPositionProvider,
        properties = PopupProperties(focusable = true),
        onDismissRequest = { finish() },
    ) {
        // 팝업 창은 기본적으로 시스템 바(상태바·내비바) 영역을 침범하지 않아 dim이 그 영역을 못 덮는다.
        // FLAG_LAYOUT_NO_LIMITS로 창을 화면 전체(시스템 바 뒤까지)로 확장 → dim이 끝까지 채워진다.
        // 이렇게 하면 팝업 원점이 edge-to-edge 메인 창 원점과 일치하므로 구멍 좌표 보정이 필요 없어진다.
        val popupView = LocalView.current
        LaunchedEffect(popupView) {
            val root = popupView.rootView
            val lp = root.layoutParams as? WindowManager.LayoutParams ?: return@LaunchedEffect
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            (popupView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .updateViewLayout(root, lp)
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = overlayAlpha.value }) {
            val screenH = constraints.maxHeight.toFloat()

            if (step.keys.isEmpty()) {
                // ── anchorless 스텝: 전체 딤 + 이미지 + 팁 (예: 위젯 홍보) ──
                // 구멍이 없어 rect 이동 애니메이션이 없으므로, 콘텐츠를 페이드+슬라이드로 등장시켜 4→5 전환을 잇는다.
                Canvas(modifier = Modifier.fillMaxSize()) { drawRect(DimColor) }

                // 이 스텝을 벗어나면 블록이 컴포지션에서 빠지므로, 재진입 때마다 다시 등장 애니메이션이 재생된다.
                val appear = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = appear,
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn(tween(280)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 10 },
                ) {
                    // 이미지·헤드라인·설명·버튼을 한 덩어리로 화면 중앙에 배치한다.
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
                        Spacer(Modifier.weight(1f))
                        if (step.image != null) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                // 2x2 위젯 미리보기 → 정사각 타일. (좌우로 늘리지 않아 여백 최소화)
                                Image(
                                    painter = painterResource(step.image),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(176.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White)
                                        .padding(12.dp),
                                )
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            GuideTipContent(index, visible.size, isLast, step, onNext = { next() }, onPrev = { prev() }, onFinish = { finish() })
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
            } else {

            // ── 앵커 있는 스텝: 구멍 + 팁 ──
            // 여러 key면 각 영역을 감싸는 union을 하나의 구멍으로
            val rectWindow = step.keys.mapNotNull { targets[it] }
                .reduceOrNull { a, r ->
                    Rect(minOf(a.left, r.left), minOf(a.top, r.top), maxOf(a.right, r.right), maxOf(a.bottom, r.bottom))
                } ?: return@BoxWithConstraints
            // 팝업이 FLAG_LAYOUT_NO_LIMITS로 메인 창과 동일한 화면 전체 좌표계를 쓰므로 보정 없이 그대로 사용
            val rect = rectWindow

            // 스텝 전환 시 구멍이 다음 요소로 부드럽게 이동·크기 변화
            val animRect by animateRectAsState(
                targetValue = rect,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                label = "spotlightHole",
            )

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
                        // 전환 중 넓은 카드에서 좁은 FAB로 올 때 원이 잠깐 거대해지지 않도록 min 사용
                        val r = minOf(animRect.width, animRect.height) / 2f * 1.35f + pad
                        drawCircle(Color.Transparent, r, animRect.center, blendMode = BlendMode.Clear)
                    }
                    SpotShape.RECT -> {
                        // 요소를 딱 감싸는 둥근 사각형 (카드·내역 모양과 일치)
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset(animRect.left - pad, animRect.top - pad),
                            size = Size(animRect.width + pad * 2, animRect.height + pad * 2),
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
                SpotShape.CIRCLE -> minOf(animRect.width, animRect.height) / 2f * 1.35f + pad
                SpotShape.RECT -> animRect.height / 2f + pad
            }
            val holeTop = animRect.center.y - holeHalfH
            val holeBottom = animRect.center.y + holeHalfH
            val gapPx = with(density) { 24.dp.toPx() }   // 구멍과 문구 사이 간격
            val sidePx = with(density) { 22.dp.toPx() }
            val below = rect.center.y < screenH * 0.5f
            val tipY = if (below) holeBottom + gapPx else (holeTop - gapPx - tipHeight)
                .coerceAtLeast(with(density) { 24.dp.toPx() })

            Column(
                modifier = Modifier
                    .offset { IntOffset(sidePx.roundToInt(), tipY.roundToInt()) }
                    // 좌측 22dp offset + 우측 22dp 여백 → 헤드라인·설명이 화면 가로를 최대로 사용
                    .width(maxWidth - 44.dp)
                    .onGloballyPositioned { tipHeight = it.size.height },
            ) {
                GuideTipContent(index, visible.size, isLast, step, onNext = { next() }, onPrev = { prev() }, onFinish = { finish() })
            }
            }
        }
    }
}

/** 스텝 번호(초록) + 헤드라인(큰 글씨) + 설명(작은 글씨) + 버튼 행. 호출부(Column) 안에서 세로로 쌓인다. */
@Composable
private fun GuideTipContent(
    index: Int,
    total: Int,
    isLast: Boolean,
    step: GuideStep,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onFinish: () -> Unit,
) {
    // 상단 줄: 스텝 번호(좌) ─ 닫기 X(우, 컴포넌트 우상단). '다음'과 멀어 오탭 방지.
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${index + 1} / $total",
            color = CounterColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.weight(1f))
        TipCloseIcon { onFinish() }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        step.title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 26.sp,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        // emphasis가 message 안에 있으면 그 부분만 에메랄드+굵게로 강조
        buildAnnotatedString {
            val emph = step.emphasis?.takeIf { it.isNotBlank() }
            val at = emph?.let { step.message.indexOf(it) } ?: -1
            if (emph != null && at >= 0) {
                append(step.message.substring(0, at))
                withStyle(SpanStyle(color = EmphasisColor, fontWeight = FontWeight.Bold)) {
                    append(emph)
                }
                append(step.message.substring(at + emph.length))
            } else {
                append(step.message)
            }
        },
        color = TipDescColor,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    )
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        val action = step.action
        if (action != null) {
            // 커스텀 액션(예: 위젯 추가) → 실행 후 가이드 종료. 이전 스텝이 있으면 뒤로도 노출.
            if (index > 0) {
                TipSecondary("‹ 뒤로") { onPrev() }
                Spacer(Modifier.width(16.dp))
            }
            TipPrimary(action.label) { action.onClick(); onFinish() }
            if (action.secondaryLabel != null) {
                Spacer(Modifier.width(16.dp))
                TipSecondary(action.secondaryLabel) { onFinish() }
            }
        } else {
            // 기본: 뒤로 · 다음/시작하기 · 건너뛰기
            if (index > 0) {
                TipSecondary("‹ 뒤로") { onPrev() }
                Spacer(Modifier.width(16.dp))
            }
            TipPrimary(if (isLast) "시작하기" else "다음 ›") { onNext() }
        }
    }
}

/** 팁 컴포넌트 우상단 닫기(X) — 스텝 번호와 같은 줄 오른쪽 끝. 탭 영역 28dp에 작은 X(14dp). */
@Composable
private fun TipCloseIcon(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .noRippleClickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val stroke = 2.dp.toPx()
            val color = Color.White.copy(alpha = 0.75f)
            drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun TipPrimary(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AmColors.Emerald)
            .noRippleClickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 9.dp),
    )
}

@Composable
private fun TipSecondary(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.62f),
        fontSize = 13.sp,
        modifier = Modifier
            .noRippleClickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp),
    )
}

private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}
