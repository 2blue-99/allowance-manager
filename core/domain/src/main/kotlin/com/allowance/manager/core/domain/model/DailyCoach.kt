package com.allowance.manager.core.domain.model

/**
 * 오늘 코치 위젯용 하루 페이스 지표. (수급일 사이클 기준)
 *
 * @param todaySpent 오늘 지출 합계
 * @param recommendedPerDay 오늘 권장 사용액 = 남은 예산 ÷ 남은 일수 (초과 시 0)
 * @param avgPerDay 지금까지 하루 평균 지출 = 사이클 지출 ÷ 경과 일수
 */
data class DailyCoach(
    val todaySpent: Long,
    val recommendedPerDay: Long,
    val avgPerDay: Long,
)
