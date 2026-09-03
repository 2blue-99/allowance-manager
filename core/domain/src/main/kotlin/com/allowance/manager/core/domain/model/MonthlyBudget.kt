package com.allowance.manager.core.domain.model

import java.time.LocalDate

/** 특정 사이클부터 적용되는 용돈(월 예산) 이력 한 건 */
data class MonthlyBudget(
    val effectiveCycle: LocalDate,
    val amount: Long,
)
