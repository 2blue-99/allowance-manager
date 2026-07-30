package com.allowance.manager.core.domain.model

import java.time.YearMonth

/** 특정 달부터 적용되는 용돈(월 예산) 이력 한 건 */
data class MonthlyBudget(
    val effectiveMonth: YearMonth,
    val amount: Long,
)
