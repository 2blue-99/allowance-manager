package com.allowance.manager.core.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 월급일 기준 이번달 사이클.
 * payday: 1~31 = 해당 일, 0 = 말일. 해당 월에 없는 날짜(예: 31 → 2월)는 말일로 clamp.
 *
 * 급여는 지급일이 주말·공휴일이면 보통 직전 영업일에 나오므로,
 * 확정된 월급일을 [adjustToBusinessDay] 로 당겨 사이클 경계(=D-day)에 반영한다.
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
        private fun resolveDay(year: Int, month: Int, payday: Int): LocalDate {
            val ym = YearMonth.of(year, month)
            val day = if (payday <= 0) ym.lengthOfMonth() else minOf(payday, ym.lengthOfMonth())
            return LocalDate.of(year, month, day)
        }

        /**
         * 지급일이 주말·공휴일이면 직전 영업일로 당긴다. (급여는 비영업일 전날 지급)
         * @param holidays 공휴일 집합. 지금은 비어 있어 주말만 보정하며, 이후 Remote Config 로 주입 예정.
         */
        fun adjustToBusinessDay(date: LocalDate, holidays: Set<LocalDate> = emptySet()): LocalDate {
            var d = date
            while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY || d in holidays) {
                d = d.minusDays(1)
            }
            return d
        }

        fun of(
            payday: Int,
            today: LocalDate = LocalDate.now(),
            holidays: Set<LocalDate> = emptySet(),
        ): BudgetCycle {
            // 월(月) 판단은 명목 월급일로 하고, 경계 값은 영업일 보정한 실지급일로 만든다.
            val thisNominal = resolveDay(today.year, today.monthValue, payday)
            val thisPay = adjustToBusinessDay(thisNominal, holidays)
            return if (!today.isBefore(thisPay)) {
                // 오늘 >= 이번달 실지급일 → 이번 사이클 = 이번달 ~ 다음달 실지급일
                val nm = thisNominal.plusMonths(1)
                val nextPay = adjustToBusinessDay(resolveDay(nm.year, nm.monthValue, payday), holidays)
                BudgetCycle(thisPay, nextPay)
            } else {
                // 오늘 < 이번달 실지급일 → 이번 사이클 = 지난달 ~ 이번달 실지급일
                val pm = thisNominal.minusMonths(1)
                val prevPay = adjustToBusinessDay(resolveDay(pm.year, pm.monthValue, payday), holidays)
                BudgetCycle(prevPay, thisPay)
            }
        }
    }
}
