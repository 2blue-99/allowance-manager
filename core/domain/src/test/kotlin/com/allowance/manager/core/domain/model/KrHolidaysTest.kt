package com.allowance.manager.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KrHolidaysTest {

    @Test
    fun `JSON 배열 형태를 파싱한다`() {
        val parsed = KrHolidays.parse("""["2026-02-16", "2026-02-17", "2026-02-18"]""")
        assertEquals(
            setOf(
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 17),
                LocalDate.of(2026, 2, 18),
            ),
            parsed,
        )
    }

    @Test
    fun `콤마 목록 형태도 파싱한다`() {
        val parsed = KrHolidays.parse("2026-01-01,2026-05-05")
        assertEquals(setOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 5)), parsed)
    }

    @Test
    fun `깨진 토큰은 버리고 나머지는 살린다`() {
        val parsed = KrHolidays.parse("""["2026-01-01", "26/01/02", "", "2026-13-45", "2026-05-05"]""")
        assertEquals(setOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 5)), parsed)
    }

    @Test
    fun `빈 값이면 빈 집합 - 호출부에서 FALLBACK으로 폴백한다`() {
        assertTrue(KrHolidays.parse("").isEmpty())
        assertTrue(KrHolidays.parse("[]").isEmpty())
    }

    @Test
    fun `기본 공휴일에 설날·추석 연휴가 들어있다`() {
        assertTrue(LocalDate.of(2026, 2, 17) in KrHolidays.FALLBACK)  // 설날
        assertTrue(LocalDate.of(2026, 9, 25) in KrHolidays.FALLBACK)  // 추석
    }
}
