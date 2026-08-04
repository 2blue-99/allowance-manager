package com.allowance.manager.core.analytics

/**
 * 앱 공통 분석 로거. 화면·이벤트·사용자속성·비정상(non-fatal) 기록을 표준화한다.
 *
 * 개인정보 가드: 파라미터에 금액(예산 제외)·계좌번호·사용처·메모·검색어 등 민감정보를 넣지 않는다.
 * 화면명·액션명·카테고리·타입·불리언 등만 기록.
 */
interface AnalyticsHelper {
    /** GA4 표준 화면 진입(screen_view) */
    fun logScreenView(screenName: String)

    /** 커스텀 이벤트. params는 String/Boolean/Int/Long/Double만 지원(그 외 toString). null 값은 무시. */
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())

    /** 사용자 속성(이후 모든 이벤트에 자동으로 따라붙어 세그먼트에 사용). value=null 이면 해제. */
    fun setUserProperty(name: String, value: String?)

    /** Crashlytics 비정상(non-fatal) 기록 */
    fun recordNonFatal(throwable: Throwable)
}
