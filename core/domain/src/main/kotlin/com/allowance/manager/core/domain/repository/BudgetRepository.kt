package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.MonthlyBudget
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 용돈(월 예산) 이력. 용돈은 사이클마다 다를 수 있어 effective-dated로 저장.
 * 키는 **사이클 시작일** — 거래·월급일 이력과 같은 축이라 명목 월을 계산할 일이 없다.
 */
interface BudgetRepository {
    /** 지정한 사이클부터 적용되도록 용돈 저장(upsert). 보통 cycleStart = 현재 사이클. */
    suspend fun setBudgetForCycle(cycleStart: LocalDate, amount: Long)

    /** 그 사이클의 용돈(이월 규칙 적용). 이력 없으면 0. */
    suspend fun getBudgetForCycle(cycleStart: LocalDate): Long

    /** 그 사이클의 용돈 Flow — 이력 변경 시 재방출. */
    fun observeBudgetForCycle(cycleStart: LocalDate): Flow<Long>

    /** 전체 용돈 이력(오래된→최신). 통계 창에 매핑. */
    fun observeHistory(): Flow<List<MonthlyBudget>>
}
