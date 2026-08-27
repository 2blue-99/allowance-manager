package com.allowance.manager.core.domain.usecase.alert

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.Holidays
import java.time.LocalDate
import java.time.YearMonth

/**
 * 월급일 알림 발송 종류 판정(순수 로직).
 *
 * 명목 월급일이 아니라 **실지급일**([BudgetCycle.payDate]) 기준이다.
 * 예: 규칙일 25일이 일요일이면 실지급일은 23일(금) → 22일(목) 밤이 [Notice.TOMORROW].
 *
 * 이웃 달(지난달·다음달)까지 같이 보는 이유:
 * - 영업일 보정으로 지급일이 이전 달로 넘어갈 수 있다. (규칙일 1일 + 8/1이 일요일 → 7/31)
 * - 오늘이 말일이면 "내일"은 다음 달의 지급일일 수 있다.
 */
object PaydayNoticeDecider {

    enum class Notice { NONE, TOMORROW, TODAY }

    fun decide(
        today: LocalDate,
        payday: Int,
        overrides: Map<YearMonth, Int> = emptyMap(),
        holidays: Holidays = Holidays.EMPTY,
    ): Notice {
        val thisYm = YearMonth.from(today)
        val payDates = listOf(thisYm.minusMonths(1), thisYm, thisYm.plusMonths(1))
            .map { BudgetCycle.payDate(it, payday, overrides, holidays) }
            .toSet()

        // 당일이 전날보다 우선 — 같은 날 둘 다 걸리는 일은 없지만 판정 순서를 못 박아둔다.
        return when {
            today in payDates -> Notice.TODAY
            today.plusDays(1) in payDates -> Notice.TOMORROW
            else -> Notice.NONE
        }
    }
}
