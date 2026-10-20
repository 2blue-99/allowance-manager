package com.allowance.manager.core.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 이번달 예산 현황. remaining = budget - spent + income (음수 = 초과).
 * spent·income 은 모두 예산 반영(scope=BUDGET) 대상만.
 */
data class BudgetStatus(
    val budget: Long,          // 월 예산 (0 = 미설정)
    val spent: Long,           // 이번달 BUDGET 지출 합계
    val income: Long,          // 이번달 BUDGET 수입 합계 (남은 금액에 가산)
    val cycle: BudgetCycle,
) {
    val remaining: Long get() = budget - spent + income
    val isOver: Boolean get() = budget > 0 && remaining < 0

    /** 남은 비율 (1=가득, 0=소진, 음수=초과). 예산 미설정 시 0. */
    val ratio: Float get() =
        if (budget <= 0) 0f else (remaining.toFloat() / budget).coerceIn(-1f, 1f)

    // ── 하루 지표 ── 홈 히어로 카드와 오늘 코치 위젯이 공유한다. 화면에서 따로 계산하지 말 것.

    /** 다음 수급일까지 남은 일수 (D-day). 수급일 당일 이후면 0 이하 */
    fun daysUntilPayday(today: LocalDate): Int =
        ChronoUnit.DAYS.between(today, cycle.nextPayday).toInt()

    /** 하루 사용 권장액 = 남은 금액 ÷ 남은 일수(최소 1). 초과면 0. 100원 단위 내림 (13,163 → 13,100) */
    fun dailyRecommended(today: LocalDate): Long {
        val daysLeft = daysUntilPayday(today).coerceAtLeast(1)
        return if (remaining > 0) remaining / daysLeft / 100 * 100 else 0L
    }

    /** 하루 평균 지출 = 지출 ÷ 경과 일수(오늘 포함, 최소 1). 100원 단위 내림 */
    fun dailyAverage(today: LocalDate): Long {
        val elapsed = (ChronoUnit.DAYS.between(cycle.start, today) + 1).coerceAtLeast(1)
        return spent / elapsed / 100 * 100
    }
}
