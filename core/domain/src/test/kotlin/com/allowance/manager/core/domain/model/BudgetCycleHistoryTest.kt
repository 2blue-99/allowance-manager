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
    fun `이력 시작일 이전은 그 규칙으로 거슬러 올라간다`() {
        // 온보딩은 그 시점 사이클 한 줄만 심는다. 역산이 없으면 과거 사이클이 아예 안 나와
        // 통계 막대가 하나만 그려지고, 과거 거래도 전부 첫 사이클로 몰린다.
        val rules = listOf(rule("2026-06-10", 10))

        // 한 사이클 전 — 2026-05-10은 일요일이라 5/8(금)로 보정된다
        BudgetCycle.of(rules, today = date("2026-05-20")).let {
            assertEquals(date("2026-05-08"), it.start)
            assertEquals(date("2026-06-10"), it.endExclusive)
        }
        // 두 사이클 전
        BudgetCycle.of(rules, today = date("2026-04-15")).let {
            assertEquals(date("2026-04-10"), it.start)
            assertEquals(date("2026-05-08"), it.endExclusive)
        }
    }

    @Test
    fun `과거로 거슬러도 사이클이 맞물린다`() {
        val rules = listOf(rule("2026-06-10", 10))
        val older = BudgetCycle.of(rules, today = date("2026-04-15"))
        val newer = BudgetCycle.of(rules, today = date("2026-05-20"))
        assertEquals(older.endExclusive, newer.start)
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

    // ── 규칙일 변경 (설정에서 월급일을 바꾼 뒤) ──
    // 변경은 현재 사이클 줄의 규칙일을 갱신하는 방식이라, 시작일은 그대로 두고 끝만 다시 계산된다.

    @Test
    fun `규칙일을 바꾸면 같은 달 안에서도 다음 지급일이 될 수 있다`() {
        // 8/1에 받고(1일 규칙) 25일로 변경 → 24일 뒤인 8/25가 다음 지급
        val cycle = BudgetCycle.of(listOf(rule("2026-08-01", 25)), today = date("2026-08-02"))
        assertEquals(date("2026-08-01"), cycle.start)
        assertEquals(date("2026-08-25"), cycle.endExclusive)
    }

    @Test
    fun `이미 지나간 규칙일은 경계가 되지 않는다`() {
        // 8/1에 받고(1일 규칙) 8/26에 10일로 변경 → 8/10은 이미 지났고 받은 적도 없다.
        // 받지 않은 날을 경계로 삼지 않으므로 다음 지급인 9/10까지가 한 사이클.
        val cycle = BudgetCycle.of(listOf(rule("2026-08-01", 10)), today = date("2026-08-26"))
        assertEquals(date("2026-08-01"), cycle.start)
        assertEquals(date("2026-09-10"), cycle.endExclusive)
    }

    @Test
    fun `영업일 보정으로 앞당겨 받은 달은 그 달 규칙일을 다시 세지 않는다`() {
        // 8/22에 받았다고 지정 + 규칙 25일 → 8/25는 3일 뒤라 같은 지급이 앞당겨진 것으로 본다
        val cycle = BudgetCycle.of(listOf(rule("2026-08-22", 25)), today = date("2026-08-23"))
        assertEquals(date("2026-08-22"), cycle.start)
        assertEquals(date("2026-09-25"), cycle.endExclusive)
    }

    @Test
    fun `이력이 비면 규칙일 하나로 계산한 결과와 같다`() {
        val fromEmpty = BudgetCycle.of(emptyList(), today = date("2026-07-20"), fallbackPayday = 10)
        val fromPayday = BudgetCycle.of(payday = 10, today = date("2026-07-20"))
        assertEquals(fromPayday.start, fromEmpty.start)
        assertEquals(fromPayday.endExclusive, fromEmpty.endExclusive)
    }
}
