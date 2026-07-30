package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.MonthlyBudget
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/**
 * 월별 용돈(월 예산) 이력. 용돈은 달마다 다를 수 있어 effective-dated로 저장.
 */
interface BudgetRepository {
    /** 지정한 달부터 적용되도록 용돈 저장(upsert). 보통 month = 이번 달. */
    suspend fun setBudgetForMonth(month: YearMonth, amount: Long)

    /** 그 달의 용돈(이월 규칙 적용). 이력 없으면 0. */
    suspend fun getBudgetForMonth(month: YearMonth): Long

    /** 그 달의 용돈 Flow — 이력 변경 시 재방출. */
    fun observeBudgetForMonth(month: YearMonth): Flow<Long>

    /** 전체 용돈 이력(오래된→최신). 통계 6개월 창에 매핑. */
    fun observeHistory(): Flow<List<MonthlyBudget>>
}
