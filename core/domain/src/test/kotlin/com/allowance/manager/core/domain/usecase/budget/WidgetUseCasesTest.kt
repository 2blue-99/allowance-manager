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
    fun `하루 권장은 오늘 0시 기준으로 고정 — 오늘 지출·수입을 되돌려 계산한다`() = runBlocking {
        val today = d(9, 15)
        val todayStart = today.millis()
        val transactions = FakeTransactionRepository().apply {
            // 사이클 지출 323,456 (오늘 12,000 포함) / 사이클 수입 50,000 (오늘 들어온 10,000 포함)
            spentBetween = { s, _ -> if (s == todayStart) 12_000L else 323_456L }
            incomeBetween = { s, _ -> if (s == todayStart) 10_000L else 50_000L }
        }
        val budgetStatus = ObserveBudgetStatusUseCase(cycles, transactions)

        val coach = ObserveDailyCoachUseCase(budgetStatus, transactions)(today = today).first()
        val status = budgetStatus(today).first()

        assertEquals(12_000L, coach.todaySpent)
        assertEquals(10, coach.daysUntilPayday)   // 9/15 → 9/25
        // 지금 남은 = 500,000 − 323,456 + 50,000 = 226,544
        // 0시 잔액 = 226,544 + 12,000(오늘 지출) − 10,000(오늘 수입) = 228,544 → ÷10일 = 22,854 → 22,800
        assertEquals(22_800L, coach.recommendedPerDay)
        assertEquals(status.dailyRecommended(today, 12_000L, 10_000L), coach.recommendedPerDay)
        assertEquals(false, coach.isOverToday)   // 12,000 < 22,800
    }

    @Test
    fun `오늘 더 써도 하루 권장은 그대로다`() = runBlocking {
        val today = d(9, 15)
        val todayStart = today.millis()
        fun coachWithTodaySpent(todaySpent: Long) = run {
            val transactions = FakeTransactionRepository().apply {
                spentBetween = { s, _ -> if (s == todayStart) todaySpent else 300_000L + todaySpent }
                incomeBetween = { _, _ -> 0L }
            }
            ObserveDailyCoachUseCase(ObserveBudgetStatusUseCase(cycles, transactions), transactions)(today = today)
        }
        val before = coachWithTodaySpent(5_000L).first()
        val after = coachWithTodaySpent(45_000L).first()   // 오늘 4만 원 더 씀

        assertEquals(before.recommendedPerDay, after.recommendedPerDay)   // 권장 고정 (0시 잔액 200,000 ÷ 10 = 20,000)
        assertEquals(20_000L, after.recommendedPerDay)
        assertEquals(false, before.isOverToday)
        assertEquals(true, after.isOverToday)   // 45,000 > 20,000
    }
}
