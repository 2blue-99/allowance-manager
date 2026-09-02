package com.allowance.manager.core.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 월급일 기준 이번달 사이클.
 * payday: 1~31 = 해당 일, 0 = 말일. 해당 월에 없는 날짜(예: 31 → 2월)는 말일로 clamp.
 *
 * 사이클 경계는 항상 [payDate] 한 곳에서만 나온다. 규칙일(payday)을 주말·공휴일 보정해서 쓴다.
 * 계산을 여기로 모아둔 덕에 홈·위젯·통계·월급일 알림이 같은 날짜를 본다.
 */
data class BudgetCycle(
    val start: LocalDate,          // 이번 수급일 (포함)
    val endExclusive: LocalDate,   // 다음 수급일 (미포함)
) {
    val nextPayday: LocalDate get() = endExclusive

    /** createdAt(epoch ms) 필터용 시작 경계 (포함) */
    fun startMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
        start.atStartOfDay(zone).toInstant().toEpochMilli()

    /** createdAt(epoch ms) 필터용 종료 경계 (포함) */
    fun endMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
        endExclusive.atStartOfDay(zone).toInstant().toEpochMilli() - 1

    companion object {
        /**
         * [ym] 달에서 [day]에 해당하는 날짜. [day] <= 0 이면 말일,
         * 그 달에 없는 날(예: 2월 31일)은 말일로 clamp.
         */
        fun dateOf(ym: YearMonth, day: Int): LocalDate {
            val resolved = if (day <= 0) ym.lengthOfMonth() else minOf(day, ym.lengthOfMonth())
            return LocalDate.of(ym.year, ym.monthValue, resolved)
        }

        /**
         * 지급일이 주말·공휴일이면 직전 영업일로 당긴다. (급여는 비영업일 전날 지급)
         * @param holidays 공휴일·은행 휴무일 목록. Remote Config(`kr_holidays`)에서 주입 — [KrHolidays] 참고.
         * 비어 있으면 주말만 보정한다.
         */
        fun adjustToBusinessDay(date: LocalDate, holidays: Holidays = Holidays.EMPTY): LocalDate {
            var d = date
            while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY || d in holidays) {
                d = d.minusDays(1)
            }
            return d
        }

        /**
         * [ym] 달의 실지급일 — 사이클 경계·D-day·월급일 알림이 공유하는 단일 진입점.
         * 규칙일([payday])을 주말·공휴일 보정해서 돌려준다.
         */
        fun payDate(
            ym: YearMonth,
            payday: Int,
            holidays: Holidays = Holidays.EMPTY,
        ): LocalDate = adjustToBusinessDay(dateOf(ym, payday), holidays)

        fun of(
            payday: Int,
            today: LocalDate = LocalDate.now(),
            holidays: Holidays = Holidays.EMPTY,
        ): BudgetCycle {
            // 월(月) 판단은 오늘이 속한 달로 하고, 경계 값은 그 달의 실지급일로 만든다.
            val thisYm = YearMonth.from(today)
            val thisPay = payDate(thisYm, payday, holidays)
            return if (!today.isBefore(thisPay)) {
                // 오늘 >= 이번달 실지급일 → 이번 사이클 = 이번달 ~ 다음달 실지급일
                val next = payDate(thisYm.plusMonths(1), payday, holidays)
                BudgetCycle(thisPay, endAfter(next, thisPay))
            } else {
                // 오늘 < 이번달 실지급일 → 이번 사이클 = 지난달 ~ 이번달 실지급일
                val prev = payDate(thisYm.minusMonths(1), payday, holidays)
                BudgetCycle(startBefore(prev, thisPay), thisPay)
            }
        }

        // 경계 단조성 가드 — 영업일 보정으로 두 경계가 같은 날이 될 수 있다.
        // 길이 0 사이클은 endMillis < startMillis 가 되어 집계가 통째로 비므로 최소 1일을 보장한다.
        private fun endAfter(candidate: LocalDate, start: LocalDate): LocalDate =
            if (candidate.isAfter(start)) candidate else start.plusDays(1)

        private fun startBefore(candidate: LocalDate, endExclusive: LocalDate): LocalDate =
            if (candidate.isBefore(endExclusive)) candidate else endExclusive.minusDays(1)
    }
}
