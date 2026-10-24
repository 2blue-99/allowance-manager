package com.allowance.manager.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 총 예산 = 기본 + 추가(생활비 반영 수입). 남은 금액·사용률·초과는 총 예산 기준 */
class BudgetStatusTest {

    private val cycle = BudgetCycle(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 11, 24))

    @Test
    fun `반영 수입이 없으면 총 예산 = 기본 예산`() {
        val s = BudgetStatus(budget = 700_000L, spent = 501_240L, income = 0L, cycle = cycle)
        assertEquals(700_000L, s.totalBudget)
        assertFalse(s.hasExtraBudget)
        assertEquals(198_760L, s.remaining)
    }

    @Test
    fun `생활비 반영 수입은 추가 예산으로 더해지고, 남은 금액과 비율은 총 예산 기준`() {
        val s = BudgetStatus(budget = 700_000L, spent = 501_240L, income = 150_000L, cycle = cycle)
        assertEquals(850_000L, s.totalBudget)
        assertTrue(s.hasExtraBudget)
        assertEquals(348_760L, s.remaining)
        assertEquals(348_760f / 850_000f, s.ratio, 0.0001f)
        assertFalse(s.isOver)
    }

    @Test
    fun `초과 판정도 총 예산 기준 — 기본 예산은 넘었지만 추가 예산으로 아직 여유`() {
        val s = BudgetStatus(budget = 700_000L, spent = 720_000L, income = 150_000L, cycle = cycle)
        assertFalse(s.isOver)
        assertEquals(130_000L, s.remaining)
    }
}
