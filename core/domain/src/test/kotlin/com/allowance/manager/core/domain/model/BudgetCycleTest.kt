package com.allowance.manager.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BudgetCycleTest {

    @Test
    fun `수급일 이전이면 지난달 수급일부터 시작`() {
        val cycle = BudgetCycle.of(payday = 25, today = LocalDate.of(2026, 7, 18))
        assertEquals(LocalDate.of(2026, 6, 25), cycle.start)
        assertEquals(LocalDate.of(2026, 7, 25), cycle.endExclusive)
    }

    @Test
    fun `수급일 당일이면 이번달 수급일부터 시작`() {
        val cycle = BudgetCycle.of(payday = 25, today = LocalDate.of(2026, 7, 25))
        assertEquals(LocalDate.of(2026, 7, 25), cycle.start)
        assertEquals(LocalDate.of(2026, 8, 25), cycle.endExclusive)
    }

    @Test
    fun `수급일 이후면 이번달 수급일부터 시작`() {
        val cycle = BudgetCycle.of(payday = 25, today = LocalDate.of(2026, 7, 30))
        assertEquals(LocalDate.of(2026, 7, 25), cycle.start)
        assertEquals(LocalDate.of(2026, 8, 25), cycle.endExclusive)
    }

    @Test
    fun `말일(0) 수급일 처리`() {
        val cycle = BudgetCycle.of(payday = 0, today = LocalDate.of(2026, 7, 18))
        assertEquals(LocalDate.of(2026, 6, 30), cycle.start)
        assertEquals(LocalDate.of(2026, 7, 31), cycle.endExclusive)
    }

    @Test
    fun `해당 월에 없는 날짜(31일)는 말일로 clamp`() {
        // 2026-02 는 28일까지 → 31 요청 시 28로 clamp
        val cycle = BudgetCycle.of(payday = 31, today = LocalDate.of(2026, 2, 10))
        assertEquals(LocalDate.of(2026, 1, 31), cycle.start)
        assertEquals(LocalDate.of(2026, 2, 28), cycle.endExclusive)
    }

    @Test
    fun `사이클 경계 millis는 start 자정부터 endExclusive 직전까지`() {
        val cycle = BudgetCycle.of(payday = 25, today = LocalDate.of(2026, 7, 30))
        // start <= end, 그리고 end 는 다음 사이클 시작 직전(-1ms)
        assert(cycle.startMillis() < cycle.endMillis())
    }
}
