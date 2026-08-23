package com.allowance.manager.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PaydayInfoTest {

    private fun info(
        month: YearMonth = YearMonth.of(2026, 8),
        rule: Int = 25,
        overrideDay: Int? = null,
        holidays: Set<LocalDate> = setOf(LocalDate.of(2026, 8, 31)),
    ) = PaydayInfo(
        month = month,
        rule = rule,
        overrideDay = overrideDay,
        actual = BudgetCycle.payDate(month, rule, overrideDay?.let { mapOf(month to it) } ?: emptyMap(), holidays),
        holidays = holidays,
    )

    @Test
    fun `지정값이 없으면 조정되지 않은 상태`() {
        assertFalse(info().isAdjusted)
        assertTrue(info(overrideDay = 21).isAdjusted)
    }

    @Test
    fun `공휴일을 입력하면 공휴일 경고`() {
        val warning = info().warningFor(31)
        assertEquals(PaydayWarning(LocalDate.of(2026, 8, 31), PaydayWarning.Kind.HOLIDAY), warning)
    }

    @Test
    fun `주말을 입력하면 주말 경고`() {
        // 2026-08-23은 일요일
        val warning = info().warningFor(23)
        assertEquals(PaydayWarning(LocalDate.of(2026, 8, 23), PaydayWarning.Kind.WEEKEND), warning)
    }

    @Test
    fun `공휴일과 주말이 겹치면 공휴일 경고가 우선`() {
        // 2026-08-22는 토요일 — 공휴일로도 지정
        val warning = info(holidays = setOf(LocalDate.of(2026, 8, 22))).warningFor(22)
        assertEquals(PaydayWarning.Kind.HOLIDAY, warning?.kind)
    }

    @Test
    fun `평일이면 경고 없음`() {
        assertNull(info().warningFor(21))   // 2026-08-21 금요일
    }

    @Test
    fun `범위를 벗어난 입력은 경고 없음`() {
        assertNull(info().warningFor(0))
        assertNull(info().warningFor(32))
    }

    @Test
    fun `그 달에 없는 날짜는 말일로 보고 판정한다`() {
        // 2026-02-31 → 2/28(토) → 주말 경고
        val warning = info(month = YearMonth.of(2026, 2), holidays = emptySet()).warningFor(31)
        assertEquals(PaydayWarning(LocalDate.of(2026, 2, 28), PaydayWarning.Kind.WEEKEND), warning)
    }
}
