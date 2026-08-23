package com.allowance.manager.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class PaydayOverridesTest {

    @Test
    fun `저장 문자열을 파싱한다`() {
        val parsed = PaydayOverrides.parse("2026-08:21,2026-09:23")
        assertEquals(mapOf(YearMonth.of(2026, 8) to 21, YearMonth.of(2026, 9) to 23), parsed)
    }

    @Test
    fun `깨진 항목과 범위를 벗어난 날짜는 버린다`() {
        val parsed = PaydayOverrides.parse("2026-08:21,2026-09,bad:1,2026-10:0,2026-11:32,2026-12:5")
        assertEquals(mapOf(YearMonth.of(2026, 8) to 21, YearMonth.of(2026, 12) to 5), parsed)
    }

    @Test
    fun `빈 값이면 빈 맵`() {
        assertTrue(PaydayOverrides.parse("").isEmpty())
    }

    @Test
    fun `format 과 parse 는 왕복한다`() {
        val map = mapOf(YearMonth.of(2026, 8) to 21, YearMonth.of(2026, 9) to 23)
        assertEquals(map, PaydayOverrides.parse(PaydayOverrides.format(map)))
    }

    @Test
    fun `null 을 넣으면 그 달 지정이 해제된다`() {
        val current = mapOf(YearMonth.of(2026, 8) to 21, YearMonth.of(2026, 9) to 23)
        val updated = PaydayOverrides.put(current, YearMonth.of(2026, 8), null)
        assertEquals(mapOf(YearMonth.of(2026, 9) to 23), updated)
    }

    @Test
    fun `보존 한도를 넘으면 오래된 달부터 버린다`() {
        // 2025-01 ~ 2026-01 = 13개월 → 가장 오래된 2025-01 이 빠지고 12개월만 남는다
        val current = (1..12).associate { YearMonth.of(2025, it) to 10 }
        val updated = PaydayOverrides.put(current, YearMonth.of(2026, 1), 11)
        assertEquals(PaydayOverrides.MAX_MONTHS, updated.size)
        assertFalse(YearMonth.of(2025, 1) in updated)
        assertTrue(YearMonth.of(2026, 1) in updated)
    }
}
