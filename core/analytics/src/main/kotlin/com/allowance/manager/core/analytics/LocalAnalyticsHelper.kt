package com.allowance.manager.core.analytics

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 컴포저블에서 이벤트를 쏘기 위한 로컬. MainActivity 루트에서 실제 구현을 주입한다.
 * 미제공 시(프리뷰·테스트) NoOp로 폴백해 크래시를 막는다.
 */
val LocalAnalyticsHelper = staticCompositionLocalOf<AnalyticsHelper> { NoOpAnalyticsHelper }

/** 미주입 환경(프리뷰 등)용 no-op */
object NoOpAnalyticsHelper : AnalyticsHelper {
    override fun logScreenView(screenName: String) {}
    override fun logEvent(name: String, params: Map<String, Any?>) {}
    override fun setUserProperty(name: String, value: String?) {}
    override fun recordNonFatal(throwable: Throwable) {}
}
