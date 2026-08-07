package com.allowance.manager.core.domain.model

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
}
