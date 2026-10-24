package com.allowance.manager.core.domain.model

/**
 * 하루 지표 — 홈 예산 카드와 오늘 코치 위젯이 같은 값을 쓴다. (수급일 사이클 기준)
 *
 * @param todaySpent 금일 지출 합계 (예산 반영 BUDGET 만, 환불은 차감)
 * @param recommendedPerDay 하루 지출 권장 — 오늘 0시 기준 고정, BudgetStatus.dailyRecommended
 * @param daysUntilPayday 다음 수급일까지 남은 일수 (0 이하 = 오늘)
 */
data class DailyCoach(
    val todaySpent: Long,
    val recommendedPerDay: Long,
    val daysUntilPayday: Int,
) {
    /** 금일 지출이 권장을 넘었는지 (권장이 0이면 이미 초과 상태라 지출이 있으면 넘은 것) */
    val isOverToday: Boolean get() = todaySpent > recommendedPerDay
}
