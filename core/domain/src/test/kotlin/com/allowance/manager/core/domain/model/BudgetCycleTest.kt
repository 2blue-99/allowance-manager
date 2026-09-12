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
    fun `payDate는 규칙일을 영업일 보정해서 준다`() {
        // 2026-07-25(토) → 7/24(금)
        assertEquals(
            LocalDate.of(2026, 7, 24),
            BudgetCycle.payDate(YearMonth.of(2026, 7), payday = 25),
        )
        // 평일이면 그대로
        assertEquals(
            LocalDate.of(2026, 8, 25),
            BudgetCycle.payDate(YearMonth.of(2026, 8), payday = 25),
        )
    }

    // ─────────── recentDate — "며칠에 받았다"를 가장 최근 지나간 날짜로 ───────────

    private val today = LocalDate.of(2026, 9, 12)

    @Test
    fun `recentDate - 이번 달에 이미 지난 날은 이번 달`() {
        assertEquals(LocalDate.of(2026, 9, 5), BudgetCycle.recentDate(5, today))
    }

    @Test
    fun `recentDate - 오늘 자체도 허용`() {
        assertEquals(LocalDate.of(2026, 9, 12), BudgetCycle.recentDate(12, today))
    }

    @Test
    fun `recentDate - 이번 달에 아직 안 온 날은 지난달`() {
        assertEquals(LocalDate.of(2026, 8, 22), BudgetCycle.recentDate(22, today))
    }

    @Test
    fun `recentDate - 이번 달에 없는 날(9월 31일)은 건너뛰고 지난달`() {
        assertEquals(LocalDate.of(2026, 8, 31), BudgetCycle.recentDate(31, today))
    }

    @Test
    fun `recentDate - 두 달 모두 없는 날이면 null`() {
        // 3/12 기준 31일: 3월(31일 있지만 미래)·2월(없음) → null
        assertEquals(null, BudgetCycle.recentDate(31, LocalDate.of(2026, 3, 12)))
    }

    @Test
    fun `recentDate - 범위 밖 숫자는 null`() {
        assertEquals(null, BudgetCycle.recentDate(0, today))
        assertEquals(null, BudgetCycle.recentDate(32, today))
    }

    // ─────────── upcomingDate — "며칠에 받을 예정"을 가장 가까운 다가올 날짜로 ───────────

    @Test
    fun `upcomingDate - 이번 달에 아직 안 온 날은 이번 달`() {
        assertEquals(LocalDate.of(2026, 9, 20), BudgetCycle.upcomingDate(20, today))
    }

    @Test
    fun `upcomingDate - 오늘은 포함하지 않고 다음 달`() {
        assertEquals(LocalDate.of(2026, 10, 12), BudgetCycle.upcomingDate(12, today))
    }

    @Test
    fun `upcomingDate - 이미 지난 날은 다음 달`() {
        assertEquals(LocalDate.of(2026, 10, 5), BudgetCycle.upcomingDate(5, today))
    }

    @Test
    fun `upcomingDate - 이번 달에 없는 날(9월 31일)은 건너뛰고 다음 달`() {
        assertEquals(LocalDate.of(2026, 10, 31), BudgetCycle.upcomingDate(31, today))
    }

    @Test
    fun `upcomingDate - 두 달 모두 없는 날이면 null`() {
        // 1/31 기준 31일: 1월(오늘이라 제외)·2월(없음) → null
        assertEquals(null, BudgetCycle.upcomingDate(31, LocalDate.of(2026, 1, 31)))
    }

    // ─────────── endAfterPayDate — 저장·미리보기가 공유하는 끝 계산 ───────────

    @Test
    fun `endAfterPayDate - 시작 뒤 첫 규칙일(보정 포함)이 끝`() {
        // 8/25 시작, 규칙 25 → 9/25(금)
        assertEquals(LocalDate.of(2026, 9, 25), BudgetCycle.endAfterPayDate(LocalDate.of(2026, 8, 25), 25))
        // 8/25 시작, 규칙 말일 → 9/30(수)
        assertEquals(LocalDate.of(2026, 9, 30), BudgetCycle.endAfterPayDate(LocalDate.of(2026, 8, 25), 0))
    }

    @Test
    fun `endAfterPayDate - 규칙을 바꿔 짧아져도 최소 열흘 미만 후보는 건너뛴다`() {
        // 8/22 시작, 규칙 25 → 8/25는 3일 뒤라 건너뛰고 9/25
        assertEquals(LocalDate.of(2026, 9, 25), BudgetCycle.endAfterPayDate(LocalDate.of(2026, 8, 22), 25))
    }
}
