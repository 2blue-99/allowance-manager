package com.allowance.manager.core.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * 한 달의 월급일 상태 — 설정의 "이번 달 월급일 조정" 행·다이얼로그가 표시할 값 묶음.
 *
 * [actual]은 사이클 경계와 **같은 계산**([BudgetCycle.payDate])에서 나온다. 화면에서 다시 계산하면
 * 홈에 보이는 기간과 어긋나므로 이 값을 그대로 쓴다.
 *
 * @param rule 규칙일(1~31, 0 = 말일) — 설정의 "월급일"
 * @param overrideDay 그 달만 지정한 날짜. null이면 규칙일로 계산 중
 * @param actual 실지급일 (지정값이 있으면 그 날짜, 없으면 규칙일을 영업일 보정한 날짜)
 */
data class PaydayInfo(
    val month: YearMonth,
    val rule: Int,
    val overrideDay: Int?,
    val actual: LocalDate,
    val holidays: Holidays,
) {
    /** 그 달만 조정된 상태인지 */
    val isAdjusted: Boolean get() = overrideDay != null

    /**
     * 다이얼로그에서 입력한 [day]에 대한 경고. 없으면 null.
     * **저장을 막지 않는다** — "그날 받았다"는 사용자가 아는 사실이고, 지정값에는 영업일 보정도 걸지 않는다.
     */
    fun warningFor(day: Int): PaydayWarning? {
        if (day !in 1..31) return null
        val date = BudgetCycle.dateOf(month, day)
        return when {
            date in holidays -> PaydayWarning(date, PaydayWarning.Kind.HOLIDAY, holidays.nameOf(date))
            date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY ->
                PaydayWarning(date, PaydayWarning.Kind.WEEKEND)
            else -> null
        }
    }
}

/**
 * 입력한 지급일이 급여가 들어오기 어려운 날일 때의 안내(에러 아님). 문구는 화면에서 만든다.
 * @param holidayName 공휴일 이름(예: "추석"). 이름을 알면 "…은 추석이에요"처럼 구체적으로 말할 수 있다.
 */
data class PaydayWarning(
    val date: LocalDate,
    val kind: Kind,
    val holidayName: String? = null,
) {
    enum class Kind { HOLIDAY, WEEKEND }
}
