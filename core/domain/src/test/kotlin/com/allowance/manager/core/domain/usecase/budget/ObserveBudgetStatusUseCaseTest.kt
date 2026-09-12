package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.usecase.FakeCycleRepository
import com.allowance.manager.core.domain.usecase.FakeTransactionRepository
import com.allowance.manager.core.domain.usecase.cycle
import com.allowance.manager.core.domain.usecase.d
import com.allowance.manager.core.domain.usecase.millis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** 예산 현황은 사이클 행의 예산 + 그 기간(createdAt 범위)의 지출·수입으로 나온다 */
class ObserveBudgetStatusUseCaseTest {

    private val cycles = FakeCycleRepository(listOf(cycle(d(8, 25), d(9, 25), budget = 500_000L)))
    private val transactions = FakeTransactionRepository()

    @Test
    fun `예산은 행에서, 지출·수입은 사이클 기간으로 조회한다`() = runBlocking {
        var askedRange: Pair<Long, Long>? = null
        transactions.spentBetween = { s, e -> askedRange = s to e; 120_000L }
        transactions.incomeBetween = { _, _ -> 30_000L }

        val status = ObserveBudgetStatusUseCase(cycles, transactions)().first()

        assertEquals(500_000L, status.budget)
        assertEquals(120_000L, status.spent)
        assertEquals(30_000L, status.income)
        assertEquals(410_000L, status.remaining)
        assertEquals(BudgetCycle(d(8, 25), d(9, 25)), status.cycle)
        // 조회 범위 = 시작 자정 ~ 끝 직전
        assertEquals(d(8, 25).millis(), askedRange!!.first)
        assertEquals(d(9, 25).millis() - 1, askedRange!!.second)
    }
}
