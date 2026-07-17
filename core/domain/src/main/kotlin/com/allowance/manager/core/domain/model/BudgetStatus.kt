package com.allowance.manager.core.domain.model

/**
 * 이번달 예산 현황. remaining = budget - spent (음수 = 초과).
 */
data class BudgetStatus(
    val budget: Long,          // 월 예산 (0 = 미설정)
    val spent: Long,           // 이번달 지출 합계
    val cycle: BudgetCycle,
) {
    val remaining: Long get() = budget - spent
    val isOver: Boolean get() = budget > 0 && spent > budget

    /** 남은 비율 (1=가득, 0=소진, 음수=초과). 예산 미설정 시 0. */
    val ratio: Float get() =
        if (budget <= 0) 0f else (remaining.toFloat() / budget).coerceIn(-1f, 1f)
}
