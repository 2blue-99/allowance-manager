package com.allowance.manager.core.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 수급일 기준 이번달 사이클.
 * payday: 1~31 = 해당 일, 0 = 말일. 해당 월에 없는 날짜(예: 31 → 2월)는 말일로 clamp.
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

        fun of(payday: Int, today: LocalDate = LocalDate.now()): BudgetCycle {
            val thisMonthPayday = resolveDay(today.year, today.monthValue, payday)
            return if (!today.isBefore(thisMonthPayday)) {
                // 오늘 >= 이번달 수급일 → 이번 사이클 = 이번달 수급일 ~ 다음달 수급일
                val nm = thisMonthPayday.plusMonths(1)
                BudgetCycle(thisMonthPayday, resolveDay(nm.year, nm.monthValue, payday))
            } else {
                // 오늘 < 이번달 수급일 → 이번 사이클 = 지난달 수급일 ~ 이번달 수급일
                val pm = thisMonthPayday.minusMonths(1)
                BudgetCycle(resolveDay(pm.year, pm.monthValue, payday), thisMonthPayday)
            }
        }
    }
}
