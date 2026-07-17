package com.allowance.manager.core.domain.model

import java.time.YearMonth

/** 특정 월의 지출 합계 (메인·무시아님·EXPENSE) */
data class MonthlyExpense(
    val yearMonth: YearMonth,
    val total: Long,
)
