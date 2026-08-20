package com.allowance.manager.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatTest {

    @Test
    fun `백만원 미만은 콤마+원`() {
        assertEquals("0원", 0L.toCompactWon())
        assertEquals("113,500원", 113_500L.toCompactWon())
        assertEquals("850,000원", 850_000L.toCompactWon())
        assertEquals("999,999원", 999_999L.toCompactWon())
    }

    @Test
    fun `백만원 경계부터 만 단위`() {
        assertEquals("100만원", 1_000_000L.toCompactWon())
        assertEquals("285만원", 2_850_000L.toCompactWon())
        assertEquals("1,500만원", 15_000_000L.toCompactWon())
    }

    @Test
    fun `만원 단위 반올림`() {
        assertEquals("285만원", 2_854_999L.toCompactWon())  // 285.4999만 → 285
        assertEquals("286만원", 2_855_000L.toCompactWon())  // 285.5만 → 286
    }

    @Test
    fun `억 단위는 억+만 조합`() {
        assertEquals("1억원", 100_000_000L.toCompactWon())
        assertEquals("3억원", 300_000_000L.toCompactWon())
        assertEquals("1억 2,345만원", 123_450_000L.toCompactWon())
    }

    @Test
    fun `억 경계로 반올림되면 억으로 올림`() {
        // 99,999,000 → 9,999.9만 → 반올림 10,000만 = 1억
        assertEquals("1억원", 99_999_000L.toCompactWon())
    }

    @Test
    fun `음수는 부호 유지`() {
        assertEquals("-24,000원", (-24_000L).toCompactWon())
        assertEquals("-240만원", (-2_400_000L).toCompactWon())
    }

    // ── toCompactShort: 절삭 + 최상위 2단위, 붙임 ──

    @Test
    fun `초압축 - 1만 미만은 천 단위 절삭`() {
        assertEquals("0", 0L.toCompactShort())
        assertEquals("약1천", 500L.toCompactShort())
        assertEquals("약1천", 999L.toCompactShort())
        assertEquals("1천", 1_000L.toCompactShort())
        assertEquals("3천", 3_900L.toCompactShort())     // 절삭
        assertEquals("9천", 9_999L.toCompactShort())
    }

    @Test
    fun `초압축 - 만 블록은 풀 + 그 아래 천 한 자리`() {
        assertEquals("1만", 10_000L.toCompactShort())
        assertEquals("8만2천", 82_000L.toCompactShort())
        assertEquals("12만8천", 128_000L.toCompactShort())
        assertEquals("8만2천", 82_900L.toCompactShort())  // 900원 절삭
        assertEquals("30만", 300_000L.toCompactShort())
        assertEquals("120만", 1_200_000L.toCompactShort())
        assertEquals("1200만", 12_000_000L.toCompactShort())
        assertEquals("9999만", 99_990_000L.toCompactShort())
    }

    @Test
    fun `초압축 - 억은 억 풀 + 그 아래 첫 단위 한 자리`() {
        assertEquals("1억", 100_000_000L.toCompactShort())
        assertEquals("1억3천만", 131_200_000L.toCompactShort())
        assertEquals("1억5백만", 105_000_000L.toCompactShort())
        assertEquals("12억", 1_200_000_000L.toCompactShort())
        assertEquals("1억", 100_009_000L.toCompactShort())   // 만 미만 잔액 절삭
    }

    @Test
    fun `초압축 - 음수는 부호 유지`() {
        assertEquals("-8만2천", (-82_000L).toCompactShort())
        assertEquals("-1억3천만", (-131_200_000L).toCompactShort())
    }
}
