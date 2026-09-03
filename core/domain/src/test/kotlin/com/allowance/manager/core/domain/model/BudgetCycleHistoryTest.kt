package com.allowance.manager.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 이력 기반 사이클 계산.
 *
 * 이력 한 줄 = "이 날 받았고, 그 이후 규칙은 payday". 사이클 시작은 저장된 날짜를 그대로 쓰고,
 * 끝은 규칙일로 계산하되 **다음 이력이 더 이르면 거기서 끊는다**(빈틈·겹침 방지).
 */
class BudgetCycleHistoryTest {

    private fun rule(date: String, payday: Int) = PaydayRule(LocalDate.parse(date), payday)
    private fun date(s: String) = LocalDate.parse(s)

    // ── 이력 한 줄 (규칙을 바꾼 적 없는 사용자) ──

    @Test
    fun `이력이 하나면 그 규칙으로 사이클이 반복된다`() {
        val rules = listOf(rule("2026-06-10", 10))

        // 시작 당일
        BudgetCycle.of(rules, today = date("2026-06-10")).let {
            assertEquals(date("2026-06-10"), it.start)
            assertEquals(date("2026-07-10"), it.endExclusive)
        }
        // 사이클 중간
        BudgetCycle.of(rules, today = date("2026-06-25")).let {
            assertEquals(date("2026-06-10"), it.start)
        }
        // 두 사이클 뒤
        BudgetCycle.of(rules, today = date("2026-08-15")).let {
            assertEquals(date("2026-08-10"), it.start)
            assertEquals(date("2026-09-10"), it.endExclusive)
        }
    }

    @Test
    fun `이력 시작일 이전을 물으면 첫 사이클을 준다`() {
        val rules = listOf(rule("2026-06-10", 10))
        val cycle = BudgetCycle.of(rules, today = date("2026-05-01"))
        assertEquals(date("2026-06-10"), cycle.start)
    }

    // ── 규칙 변경 (이력 두 줄) ──

    @Test
    fun `규칙을 바꾸면 그 날짜에서 이전 사이클이 끊긴다`() {
        // 6/10부터 10일 규칙 → 8/22에 받고 이후 25일 규칙
        val rules = listOf(rule("2026-06-10", 10), rule("2026-08-22", 25))

        // 변경 직전 사이클: 8/10 시작인데 8/22에 새 이력이 있어 거기서 끝난다
        BudgetCycle.of(rules, today = date("2026-08-15")).let {
            assertEquals(date("2026-08-10"), it.start)
            assertEquals(date("2026-08-22"), it.endExclusive)
        }
        // 변경 이후: 8/22 시작, 다음은 25일 규칙으로 9/25
        BudgetCycle.of(rules, today = date("2026-09-02")).let {
            assertEquals(date("2026-08-22"), it.start)
            assertEquals(date("2026-09-25"), it.endExclusive)
        }
    }

    @Test
    fun `규칙 변경 전 과거를 물으면 그때 규칙으로 계산한다`() {
        val rules = listOf(rule("2026-06-10", 10), rule("2026-08-22", 25))
        // 7월은 아직 10일 규칙
        val cycle = BudgetCycle.of(rules, today = date("2026-07-20"))
        assertEquals(date("2026-07-10"), cycle.start)
        assertEquals(date("2026-08-10"), cycle.endExclusive)
    }

    @Test
    fun `사이클이 끊기고 이어져도 빈틈이 없다`() {
        val rules = listOf(rule("2026-06-10", 10), rule("2026-08-22", 25))
        val before = BudgetCycle.of(rules, today = date("2026-08-15"))
        val after = BudgetCycle.of(rules, today = date("2026-08-22"))
        // 앞 사이클의 끝(미포함) == 뒤 사이클의 시작
        assertEquals(before.endExclusive, after.start)
    }

    // ── 영업일 보정 ──

    @Test
    fun `다음 경계는 주말이면 직전 영업일로 당긴다`() {
        // 2026-07-25는 토요일 → 7/24(금)
        val rules = listOf(rule("2026-06-25", 25))
        val cycle = BudgetCycle.of(rules, today = date("2026-07-01"))
        assertEquals(date("2026-07-24"), cycle.endExclusive)
    }

    @Test
    fun `이력에 저장된 시작일은 주말이어도 보정하지 않는다`() {
        // 2026-08-23(일)에 받았다고 사용자가 지정한 경우 — 그대로 경계가 된다
        val rules = listOf(rule("2026-08-23", 25))
        val cycle = BudgetCycle.of(rules, today = date("2026-08-24"))
        assertEquals(date("2026-08-23"), cycle.start)
    }

    // ── 말일 규칙 ──

    @Test
    fun `말일 규칙은 달마다 길이가 달라도 맞는다`() {
        val rules = listOf(rule("2026-01-31", 0))
        val cycle = BudgetCycle.of(rules, today = date("2026-02-10"))
        assertEquals(date("2026-01-31"), cycle.start)
        // 2026-02-28은 토요일 → 2/27(금)
        assertEquals(date("2026-02-27"), cycle.endExclusive)
    }

    // ── 경계 가드 ──

    @Test
    fun `사이클 길이는 최소 하루를 보장한다`() {
        val rules = listOf(rule("2026-06-10", 10))
        val cycle = BudgetCycle.of(rules, today = date("2026-06-10"))
        assert(cycle.endExclusive.isAfter(cycle.start))
        assert(cycle.startMillis() < cycle.endMillis())
    }

    @Test
    fun `이력이 비면 규칙일 하나로 계산한 결과와 같다`() {
        val fromEmpty = BudgetCycle.of(emptyList(), today = date("2026-07-20"), fallbackPayday = 10)
        val fromPayday = BudgetCycle.of(payday = 10, today = date("2026-07-20"))
        assertEquals(fromPayday.start, fromEmpty.start)
        assertEquals(fromPayday.endExclusive, fromEmpty.endExclusive)
    }
}
