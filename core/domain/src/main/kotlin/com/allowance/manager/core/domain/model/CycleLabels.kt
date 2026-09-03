package com.allowance.manager.core.domain.model

import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 사이클 표기·매핑 확장. 화면마다 다시 만들지 말고 여기 것을 쓴다.
 *
 * 사이클에는 "몇 월"이라는 이름이 없다(수급일이 1일이면 달력 월과 같고, 말일이면 거의 다음 달이며,
 * 규칙을 바꾸면 한 달에 둘이 생기기도 한다). 그래서 화면에는 **날짜 범위를 그대로** 적는다.
 */

/** 사이클의 마지막 날 (다음 받는 날 전날) */
val BudgetCycle.lastDay: java.time.LocalDate get() = endExclusive.minusDays(1)

/** 짧은 표기 — "8.25 – 9.24". 폭이 좁은 홈 카드·위젯용 */
fun BudgetCycle.toShortPeriodLabel(): String =
    "${start.monthValue}.${start.dayOfMonth} – ${lastDay.monthValue}.${lastDay.dayOfMonth}"

/** 긴 표기 — "26. 8. 25 – 26. 9. 24". 연도까지 밝히는 월별·통계 헤더용 */
fun BudgetCycle.toPeriodLabel(): String =
    "${start.year % 100}. ${start.monthValue}. ${start.dayOfMonth}" +
        " – ${lastDay.year % 100}. ${lastDay.monthValue}. ${lastDay.dayOfMonth}"

/**
 * 사이클의 대표 월 — **중간 날짜가 속한 달**. 월 피커에서 달 ↔ 사이클을 잇는 데 쓴다.
 *
 * 1일에 받으면 그 달, 말일에 받으면 다음 달이 되어 사람이 느끼는 감각과 맞는다.
 * 규칙을 바꾼 달에는 두 사이클의 대표 월이 겹칠 수 있는데, 그때는 최신 것을 택한다([byRepresentativeMonth]).
 */
val BudgetCycle.representativeMonth: YearMonth
    get() = YearMonth.from(start.plusDays(ChronoUnit.DAYS.between(start, endExclusive) / 2))

/**
 * 달 → 사이클 맵. 대표 월이 겹치면 **최신 사이클**이 남는다
 * (오름차순 정렬 뒤 `associateBy`라 나중 것이 앞을 덮는다).
 */
fun List<BudgetCycle>.byRepresentativeMonth(): Map<YearMonth, BudgetCycle> =
    sortedBy { it.start }.associateBy { it.representativeMonth }
