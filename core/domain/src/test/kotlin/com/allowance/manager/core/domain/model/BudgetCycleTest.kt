package com.allowance.manager.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetCycleTest {

    // ── 월 선택 로직 (평일 월급일로 고정해 영업일 보정 영향 배제) ──
    // 2026년 6/10(수)·7/10(금)·8/10(월) 은 모두 평일.

    @Test
    fun `월급일 이전이면 지난달 월급일부터 시작`() {
        val cycle = BudgetCycle.of(payday = 10, today = LocalDate.of(2026, 7, 5))
        assertEquals(LocalDate.of(2026, 6, 10), cycle.start)
        assertEquals(LocalDate.of(2026, 7, 10), cycle.endExclusive)
    }

    @Test
    fun `월급일 당일이면 이번달 월급일부터 시작`() {
        val cycle = BudgetCycle.of(payday = 10, today = LocalDate.of(2026, 7, 10))
        assertEquals(LocalDate.of(2026, 7, 10), cycle.start)
        assertEquals(LocalDate.of(2026, 8, 10), cycle.endExclusive)
    }

    @Test
    fun `월급일 이후면 이번달 월급일부터 시작`() {
        val cycle = BudgetCycle.of(payday = 10, today = LocalDate.of(2026, 7, 20))
        assertEquals(LocalDate.of(2026, 7, 10), cycle.start)
        assertEquals(LocalDate.of(2026, 8, 10), cycle.endExclusive)
    }

    @Test
    fun `말일(0) 월급일 처리`() {
        // 2026-06-30(화)·07-31(금) 모두 평일
        val cycle = BudgetCycle.of(payday = 0, today = LocalDate.of(2026, 7, 18))
        assertEquals(LocalDate.of(2026, 6, 30), cycle.start)
        assertEquals(LocalDate.of(2026, 7, 31), cycle.endExclusive)
    }

    @Test
    fun `해당 월에 없는 날짜(31일)는 말일로 clamp`() {
        // 2026-04 는 30일까지 → 31 요청 시 30(목)으로 clamp. 3-31(화)·4-30(목) 평일.
        val cycle = BudgetCycle.of(payday = 31, today = LocalDate.of(2026, 4, 10))
        assertEquals(LocalDate.of(2026, 3, 31), cycle.start)
        assertEquals(LocalDate.of(2026, 4, 30), cycle.endExclusive)
    }

    // ── 영업일 보정 ──

    @Test
    fun `지급일이 토요일이면 직전 금요일로 당긴다`() {
        // 2026-07-25 는 토요일 → 07-24(금)
        val cycle = BudgetCycle.of(payday = 25, today = LocalDate.of(2026, 7, 30))
        assertEquals(LocalDate.of(2026, 7, 24), cycle.start)
        assertEquals(LocalDate.of(2026, 8, 25), cycle.endExclusive) // 8-25(화)는 그대로
    }

    @Test
    fun `지급일이 일요일이면 직전 금요일로 당긴다`() {
        // 2026-06-21 는 일요일 → 06-19(금)
        val cycle = BudgetCycle.of(payday = 21, today = LocalDate.of(2026, 6, 25))
        assertEquals(LocalDate.of(2026, 6, 19), cycle.start)
        assertEquals(LocalDate.of(2026, 7, 21), cycle.endExclusive) // 7-21(화)는 그대로
    }

    @Test
    fun `보정으로 오늘이 실지급일 이후가 되면 이번 사이클로 넘어간다`() {
        // 명목 월급일 07-25(토)→실지급 07-24(금). 오늘 07-24 이면 이미 이번 사이클.
        val cycle = BudgetCycle.of(payday = 25, today = LocalDate.of(2026, 7, 24))
        assertEquals(LocalDate.of(2026, 7, 24), cycle.start)
        assertEquals(LocalDate.of(2026, 8, 25), cycle.endExclusive)
    }

    @Test
    fun `공휴일 집합을 주면 공휴일도 직전 영업일로 당긴다`() {
        // 08-25(화)는 평일이지만 공휴일로 지정 → 08-24(월)
        val cycle = BudgetCycle.of(
            payday = 25,
            today = LocalDate.of(2026, 8, 30),
            holidays = Holidays(mapOf(LocalDate.of(2026, 8, 25) to "임시공휴일")),
        )
        assertEquals(LocalDate.of(2026, 8, 24), cycle.start)
        assertEquals(LocalDate.of(2026, 9, 25), cycle.endExclusive) // 9-25(금)는 그대로
    }

    @Test
    fun `사이클 경계 millis는 start 자정부터 endExclusive 직전까지`() {
        val cycle = BudgetCycle.of(payday = 10, today = LocalDate.of(2026, 7, 20))
        // start <= end, 그리고 end 는 다음 사이클 시작 직전(-1ms)
        assert(cycle.startMillis() < cycle.endMillis())
    }

    @Test
    fun `그 달만 조정하면 사이클 시작이 그 날로 옮겨진다`() {
        // 8월만 21일로 조정 → 8/23은 이미 새 사이클(8/21~) 안
        val cycle = BudgetCycle.of(
            payday = 25,
            today = LocalDate.of(2026, 8, 23),
            overrides = mapOf(YearMonth.of(2026, 8) to 21),
        )
        assertEquals(LocalDate.of(2026, 8, 21), cycle.start)
        assertEquals(LocalDate.of(2026, 9, 25), cycle.endExclusive) // 다음 달은 규칙일로 복귀
    }

    @Test
    fun `조정한 날짜가 오늘보다 뒤면 아직 이전 사이클`() {
        // 8월을 28일로 조정 → 8/26은 아직 7월 사이클
        val cycle = BudgetCycle.of(
            payday = 25,
            today = LocalDate.of(2026, 8, 26),
            overrides = mapOf(YearMonth.of(2026, 8) to 28),
        )
        assertEquals(LocalDate.of(2026, 7, 24), cycle.start) // 7/25(토) → 7/24(금)
        assertEquals(LocalDate.of(2026, 8, 28), cycle.endExclusive)
    }

    @Test
    fun `조정한 날짜는 주말·공휴일 보정을 하지 않는다`() {
        // 2026-08-23은 일요일 — 사용자가 지정했으면 그대로 경계가 된다
        val cycle = BudgetCycle.of(
            payday = 25,
            today = LocalDate.of(2026, 8, 24),
            overrides = mapOf(YearMonth.of(2026, 8) to 23),
        )
        assertEquals(LocalDate.of(2026, 8, 23), cycle.start)
    }

    @Test
    fun `조정과 영업일 보정이 겹쳐도 사이클 길이가 0이 되지 않는다`() {
        // 규칙일 1일: 2026-11-01(일) → 보정하면 10/30(금).
        // 10월을 30일로 지정하면 두 경계가 같은 날이 될 수 있다 → 최소 1일 보장
        val cycle = BudgetCycle.of(
            payday = 1,
            today = LocalDate.of(2026, 10, 30),
            overrides = mapOf(YearMonth.of(2026, 10) to 30),
        )
        assertEquals(LocalDate.of(2026, 10, 30), cycle.start)
        assert(cycle.endExclusive.isAfter(cycle.start))
        assert(cycle.startMillis() < cycle.endMillis())
    }

    @Test
    fun `payDate는 조정값을 그대로 규칙일은 보정해서 준다`() {
        val overrides = mapOf(YearMonth.of(2026, 8) to 21)
        assertEquals(
            LocalDate.of(2026, 8, 21),
            BudgetCycle.payDate(YearMonth.of(2026, 8), payday = 25, overrides = overrides),
        )
        // 조정 없는 달: 2026-07-25(토) → 7/24(금)
        assertEquals(
            LocalDate.of(2026, 7, 24),
            BudgetCycle.payDate(YearMonth.of(2026, 7), payday = 25, overrides = overrides),
        )
    }
}
