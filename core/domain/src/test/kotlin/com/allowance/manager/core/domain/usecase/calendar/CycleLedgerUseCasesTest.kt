package com.allowance.manager.core.domain.usecase.calendar

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.usecase.FakeCycleRepository
import com.allowance.manager.core.domain.usecase.FakeRemoteConfigRepository
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

    private val adjacent = GetAdjacentCycleUseCase(cycles, FakeRemoteConfigRepository())

    // 첫 행(6/25~) 앞의 가상 사이클 — 규칙 25일 역산: 5/25(월), 4/24(4/25 토 보정)
    private val v1 = BudgetCycle(d(5, 25), d(6, 25))
    private val v2 = BudgetCycle(d(4, 24), d(5, 25))

    @Test
    fun `이웃 행으로 이동한다`() = runBlocking {
        assertEquals(a.period, adjacent(b.period, -1))
        assertEquals(c.period, adjacent(b.period, +1))
        assertEquals(c.period, adjacent(a.period, +2))
    }

    @Test
    fun `미래로는 마지막 행에서 멈춘다 - 호출부는 시작일이 같으면 정지로 본다`() = runBlocking {
        assertEquals(c.period, adjacent(c.period, +1))
        assertEquals(c.period, adjacent(c.period, +3))
    }

    @Test
    fun `첫 행보다 앞은 규칙으로 역산한 가상 사이클로 이어진다 - 통계 창이 항상 6칸`() = runBlocking {
        assertEquals(v1, adjacent(a.period, -1))
        assertEquals(v2, adjacent(a.period, -2))
        // 현재(c)에서 5칸 뒤 = 행 2개 + 가상 3개
        val fifth = adjacent(c.period, -5)
        assertEquals(d(3, 25), fifth.start)   // 3/25(수)
        assertEquals(d(4, 24), fifth.endExclusive)
    }

    @Test
    fun `가상 사이클에서 앞뒤로 이어진다`() = runBlocking {
        assertEquals(a.period, adjacent(v1, +1))
        assertEquals(v2, adjacent(v1, -1))
        assertEquals(b.period, adjacent(v2, +3))
    }

    @Test
    fun `offset 0은 그대로`() = runBlocking {
        assertEquals(b.period, adjacent(b.period, 0))
    }

    @Test
    fun `매트릭스 - 규칙 9종 모두 행 하나에서 5칸 뒤로 가고 돌아오면 제자리, 체인이 맞물린다`() = runBlocking {
        val today = d(9, 12)
        for (payday in listOf(0, 1, 5, 10, 15, 20, 25, 28, 31)) {
            val current = BudgetCycle.of(payday, today)
            val uc = GetAdjacentCycleUseCase(
                FakeCycleRepository(listOf(cycle(current.start, current.endExclusive, payday = payday))),
                FakeRemoteConfigRepository(),
            )
            val window = ArrayDeque(listOf(current))
            repeat(5) { window.addFirst(uc(window.first(), -1)) }
            assertEquals("규칙=$payday 6칸", 6, window.distinctBy { it.start }.size)
            window.zipWithNext().forEach { (a, b) -> assertEquals("규칙=$payday 맞물림", a.endExclusive, b.start) }
            var back = window.first()
            repeat(5) { back = uc(back, +1) }
            assertEquals("규칙=$payday 왕복", current, back)
            assertEquals("규칙=$payday 미래 멈춤", current, uc(current, +1))
        }
    }

    @Test
    fun `행이 하나뿐(새 사용자)이어도 5칸 뒤까지 간다`() = runBlocking {
        val single = GetAdjacentCycleUseCase(FakeCycleRepository(listOf(c)), FakeRemoteConfigRepository())
        var cur = c.period
        repeat(5) { cur = single(cur, -1) }
        assertEquals(d(3, 25), cur.start)
        // 왕복하면 제자리
        var back = cur
        repeat(5) { back = single(back, +1) }
        assertEquals(c.period, back)
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
