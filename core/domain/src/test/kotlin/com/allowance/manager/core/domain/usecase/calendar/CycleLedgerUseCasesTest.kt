package com.allowance.manager.core.domain.usecase.calendar

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

class CycleLedgerUseCasesTest {

    private val a = cycle(d(6, 25), d(7, 24))
    private val b = cycle(d(7, 24), d(8, 25))
    private val c = cycle(d(8, 25), d(9, 25))
    private val cycles = FakeCycleRepository(listOf(a, b, c))

    // ─────────────────────────── 이웃 사이클 ───────────────────────────

    private val adjacent = GetAdjacentCycleUseCase(cycles)

    @Test
    fun `이웃 행으로 이동한다`() = runBlocking {
        assertEquals(a.period, adjacent(b.period, -1))
        assertEquals(c.period, adjacent(b.period, +1))
        assertEquals(c.period, adjacent(a.period, +2))
    }

    @Test
    fun `목록 끝에서는 멈춘다 - 호출부는 시작일이 같으면 정지로 본다`() = runBlocking {
        assertEquals(a.period, adjacent(a.period, -1))
        assertEquals(c.period, adjacent(c.period, +3))
    }

    @Test
    fun `offset 0은 그대로`() = runBlocking {
        assertEquals(b.period, adjacent(b.period, 0))
    }

    @Test
    fun `행에 없는 가상 과거 사이클에서 앞으로 가면 첫 행`() = runBlocking {
        val virtual = BudgetCycle(d(5, 25), d(6, 25))
        assertEquals(a.period, adjacent(virtual, +1))
        assertEquals(a.period, adjacent(virtual, -1))
    }

    // ─────────────────────────── 거래 있는 사이클 ───────────────────────────

    @Test
    fun `거래 시각이 기간 안에 있는 사이클만, 최신순으로`() = runBlocking {
        val transactions = FakeTransactionRepository().apply {
            allTimes = listOf(d(9, 1).millis(), d(7, 30).millis())
        }

        val result = ObserveTransactionCyclesUseCase(cycles, transactions)().first()

        assertEquals(listOf(c.period, b.period), result)
    }

    @Test
    fun `거래가 없으면 빈 목록`() = runBlocking {
        val result = ObserveTransactionCyclesUseCase(cycles, FakeTransactionRepository())().first()
        assertEquals(emptyList<BudgetCycle>(), result)
    }

    @Test
    fun `경계 날짜의 거래는 그 날 시작하는 사이클에 속한다`() = runBlocking {
        val transactions = FakeTransactionRepository().apply { allTimes = listOf(d(8, 25).millis()) }

        val result = ObserveTransactionCyclesUseCase(cycles, transactions)().first()

        assertEquals(listOf(c.period), result)
    }

    // ─────────────────────────── 사이클 기간 내역 ───────────────────────────

    @Test
    fun `사이클 내역은 시작 자정부터 끝 직전까지로 조회한다`() = runBlocking {
        var asked: Pair<Long, Long>? = null
        val transactions = FakeTransactionRepository().apply { between = { s, e -> asked = s to e; emptyList() } }

        ObserveCycleTransactionsUseCase(transactions)(b.period).first()

        assertEquals(d(7, 24).millis(), asked!!.first)
        assertEquals(d(8, 25).millis() - 1, asked!!.second)
    }
}
