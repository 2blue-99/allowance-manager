package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.usecase.FakeCycleRepository
import com.allowance.manager.core.domain.usecase.FakeTransactionRepository
import com.allowance.manager.core.domain.usecase.calendar.ObserveMonthlyLedgerUseCase
import com.allowance.manager.core.domain.usecase.cycle
import com.allowance.manager.core.domain.usecase.d
import com.allowance.manager.core.domain.usecase.millis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** 위젯은 홈과 같은 사이클·같은 계산을 써야 한다 (가계부 위젯 = 사이클 범위, 오늘 코치 = 홈 하루 지표) */
class WidgetUseCasesTest {

    // 수급일 25일: 8/25 ~ 9/25 사이클, 그 다음 9/25 ~ 10/25
    private val cycles = FakeCycleRepository(
        listOf(
            cycle(d(8, 25), d(9, 25), budget = 500_000L),
            cycle(d(9, 25), d(10, 25), budget = 600_000L),
        )
    )

    @Test
    fun `가계부 위젯은 달력 월이 아니라 오늘이 속한 수급일 사이클 범위로 합산한다`() = runBlocking {
        var asked: Pair<Long, Long>? = null
        val transactions = object : FakeTransactionRepository() {
            override fun observeLedgerSpentBetween(start: Long, end: Long): Flow<Long> {
                asked = start to end
                return flowOf(70_000L)
            }
            override fun observeLedgerIncomeBetween(start: Long, end: Long): Flow<Long> = flowOf(20_000L)
        }

        val ledger = ObserveMonthlyLedgerUseCase(cycles, transactions)(today = d(9, 3)).first()

        assertEquals(70_000L, ledger.expense)
        assertEquals(20_000L, ledger.income)
        // 9/3은 8/25 사이클 — 달력 월(9/1~9/30)이 아님
        assertEquals(d(8, 25).millis(), asked!!.first)
        assertEquals(d(9, 25).millis() - 1, asked!!.second)
    }

    @Test
    fun `수급일이 지나면 가계부 위젯은 새 사이클로 넘어간다`() = runBlocking {
        var askedStart: Long? = null
        val transactions = object : FakeTransactionRepository() {
            override fun observeLedgerSpentBetween(start: Long, end: Long): Flow<Long> {
                askedStart = start
                return flowOf(0L)
            }
            override fun observeLedgerIncomeBetween(start: Long, end: Long): Flow<Long> = flowOf(0L)
        }

        ObserveMonthlyLedgerUseCase(cycles, transactions)(today = d(9, 26)).first()

        assertEquals(d(9, 25).millis(), askedStart)
    }

    @Test
    fun `오늘 코치는 홈과 같은 하루 지표를 쓴다 — 수입 가산·100원 내림`() = runBlocking {
        val today = d(9, 15)
        val todayStart = today.millis()
        val transactions = FakeTransactionRepository().apply {
            // 사이클 지출 323,456 / 오늘 지출 12,000 / 수입 50,000
            spentBetween = { s, _ -> if (s == todayStart) 12_000L else 323_456L }
            incomeBetween = { _, _ -> 50_000L }
        }
        val budgetStatus = ObserveBudgetStatusUseCase(cycles, transactions)

        val coach = ObserveDailyCoachUseCase(budgetStatus, transactions)(today = today).first()
        val status = budgetStatus(today).first()

        assertEquals(12_000L, coach.todaySpent)
        // 남은 = 500,000 − 323,456 + 50,000 = 226,544, 남은 일수 = 9/15→9/25 = 10일 → 22,654 → 22,600
        assertEquals(22_600L, coach.recommendedPerDay)
        // 경과 = 8/25~9/15 = 22일 → 323,456 / 22 = 14,702 → 14,700
        assertEquals(14_700L, coach.avgPerDay)
        // 홈 히어로 카드와 동일한 값
        assertEquals(status.dailyRecommended(today), coach.recommendedPerDay)
        assertEquals(status.dailyAverage(today), coach.avgPerDay)
    }
}
