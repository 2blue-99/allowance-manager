package com.allowance.manager.core.domain.model

/**
 * 오늘 코치 위젯용 하루 페이스 지표. (수급일 사이클 기준)
 *
 * @param todaySpent 오늘 지출 합계
 * @param recommendedPerDay 오늘 권장 사용액 — BudgetStatus.dailyRecommended (홈과 동일)
 * @param avgPerDay 지금까지 하루 평균 지출 — BudgetStatus.dailyAverage (홈과 동일)
 */
data class DailyCoach(
    val todaySpent: Long,
    val recommendedPerDay: Long,
    val avgPerDay: Long,
)
