package com.allowance.manager.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KrHolidaysTest {

    @Test
    fun `날짜와 이름을 파싱하고 버전을 읽는다`() {
        val parsed = KrHolidays.parse(
            """
            {
              "version": 7,
              "holidays": [
                { "date": "2026-02-17", "name": "설날", "type": "법정공휴일", "dayOfWeek": "화" },
                { "date": "2026-05-01", "name": "근로자의 날", "type": "은행휴무", "dayOfWeek": "금" }
              ]
            }
            """.trimIndent(),
        )
        assertEquals(7, parsed.version)
        assertEquals(2, parsed.count)
        assertEquals("설날", parsed.nameOf(LocalDate.of(2026, 2, 17)))
        assertEquals("근로자의 날", parsed.nameOf(LocalDate.of(2026, 5, 1)))
        assertTrue(LocalDate.of(2026, 2, 17) in parsed)
    }

    @Test
    fun `모르는 필드는 무시한다`() {
        val parsed = KrHolidays.parse(
            """{"version":1,"updatedAt":"2026-08-24","description":"x",
               "holidays":[{"date":"2026-01-01","name":"신정","type":"법정공휴일","dayOfWeek":"목","note":"y"}]}""",
        )
        assertEquals(1, parsed.count)
        assertEquals("신정", parsed.nameOf(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `깨진 날짜 항목만 버리고 나머지는 살린다`() {
        val parsed = KrHolidays.parse(
            """{"holidays":[{"date":"2026-01-01","name":"신정"},{"date":"26/01/02","name":"엉터리"},
               {"date":"2026-13-45","name":"엉터리"},{"date":"2026-05-05","name":"어린이날"}]}""",
        )
        assertEquals(2, parsed.count)
        assertTrue(LocalDate.of(2026, 5, 5) in parsed)
    }

    @Test
    fun `빈 값·깨진 JSON이면 빈 결과 - 호출부가 FALLBACK으로 넘어간다`() {
        assertTrue(KrHolidays.parse("").count == 0)
        assertTrue(KrHolidays.parse("{").count == 0)
        assertTrue(KrHolidays.parse("""{"holidays":[]}""").count == 0)
        assertTrue(KrHolidays.parse("2026-01-01,2026-05-05").count == 0)  // 옛 CSV 형식은 더 이상 안 받는다
    }

    @Test
    fun `원격 값이 없을 때 쓰는 폴백은 version이 0이다`() {
        assertEquals(0, KrHolidays.FALLBACK.version)
    }

    @Test
    fun `폴백에 명절·대체공휴일·근로자의 날이 들어있다`() {
        val f = KrHolidays.FALLBACK
        assertEquals("설날", f.nameOf(LocalDate.of(2026, 2, 17)))
        assertEquals("추석", f.nameOf(LocalDate.of(2026, 9, 25)))
        assertEquals("근로자의 날", f.nameOf(LocalDate.of(2026, 5, 1)))
        assertEquals("대체공휴일 (광복절)", f.nameOf(LocalDate.of(2026, 8, 17)))
        // 2028년까지 담고 있다
        assertEquals("설날", f.nameOf(LocalDate.of(2028, 1, 27)))
    }

    @Test
    fun `설날·추석 연휴가 토요일과 겹칠 때는 대체공휴일이 없다`() {
        // 2026 추석 연휴 마지막 날 9/26은 토요일 — 설날·추석은 일요일과 겹칠 때만 대체공휴일
        assertNull(KrHolidays.FALLBACK.nameOf(LocalDate.of(2026, 9, 28)))
    }
}
