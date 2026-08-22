package com.allowance.manager.core.designsystem.anim

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * 앱 공통 모션 스펙. 지속시간·전환 애니메이션을 한곳에서 관리한다.
 * - NavHost 화면 전환: [fadeEnter] / [fadeExit]
 * - AnimatedContent 전환: [slideForward] / [slideBackward] / [fadeThrough]
 */
object AmMotion {
    /** 기본 전환 지속시간(ms) */
    const val DurationMs = 250

    /** 빠른 페이드 지속시간(ms) — 바텀 탭 등 잦은 화면 전환용 (크로스페이드 체감 지연 완화) */
    const val FastDurationMs = 200

    /** 화면 페이드 인 (NavHost enter 등) */
    fun fadeEnter(durationMs: Int = DurationMs): EnterTransition =
        fadeIn(tween(durationMs))

    /** 화면 페이드 아웃 (NavHost exit 등) */
    fun fadeExit(durationMs: Int = DurationMs): ExitTransition =
        fadeOut(tween(durationMs))

    /** NavHost enter: 오른쪽에서 슬라이드 인 + 페이드 (앞으로 진행 — 온보딩 완료 → 홈 등) */
    fun slideForwardEnter(durationMs: Int = DurationMs): EnterTransition =
        slideInHorizontally(tween(durationMs)) { it } + fadeIn(tween(durationMs))

    /** NavHost exit: 왼쪽으로 슬라이드 아웃 + 페이드 */
    fun slideForwardExit(durationMs: Int = DurationMs): ExitTransition =
        slideOutHorizontally(tween(durationMs)) { -it } + fadeOut(tween(durationMs))

    /** NavHost popEnter: 왼쪽에서 슬라이드 인 + 페이드 (뒤로가기로 이전 화면 복귀) */
    fun slideBackwardEnter(durationMs: Int = DurationMs): EnterTransition =
        slideInHorizontally(tween(durationMs)) { -it } + fadeIn(tween(durationMs))

    /** NavHost popExit: 오른쪽으로 슬라이드 아웃 + 페이드 (뒤로가기로 현재 화면 이탈) */
    fun slideBackwardExit(durationMs: Int = DurationMs): ExitTransition =
        slideOutHorizontally(tween(durationMs)) { it } + fadeOut(tween(durationMs))

    /** AnimatedContent: 가로 슬라이드 + 페이드 (앞으로 진행 — 새 화면은 오른쪽에서, 기존은 왼쪽으로) */
    fun slideForward(durationMs: Int = DurationMs): ContentTransform =
        (slideInHorizontally(tween(durationMs)) { it } + fadeEnter(durationMs)) togetherWith
            (slideOutHorizontally(tween(durationMs)) { -it } + fadeExit(durationMs))

    /** AnimatedContent: 가로 슬라이드 + 페이드 (뒤로 진행 — 새 화면은 왼쪽에서, 기존은 오른쪽으로) */
    fun slideBackward(durationMs: Int = DurationMs): ContentTransform =
        (slideInHorizontally(tween(durationMs)) { -it } + fadeEnter(durationMs)) togetherWith
            (slideOutHorizontally(tween(durationMs)) { it } + fadeExit(durationMs))

    /** AnimatedContent: 페이드만 */
    fun fadeThrough(durationMs: Int = DurationMs): ContentTransform =
        fadeEnter(durationMs) togetherWith fadeExit(durationMs)
}
