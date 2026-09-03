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

/**
 * 사이클 기간 표기 — **"26. 8. 25 ~ 26. 9. 24"**.
 *
 * 월별·통계 헤더용. 과거를 오가는 화면이라 몇 년 것인지 밝혀야 한다.
 */
fun BudgetCycle.toPeriodLabel(): String = "${start.dateText()} ~ ${lastDay.dateText()}"

/**
 * 연도를 뺀 기간 표기 — **"8. 25 ~ 9. 24"**.
 *
 * 홈·위젯용. 항상 현재 사이클만 보여주므로 연도가 자명하고, 그만큼 폭이 빠듯하다.
 */
fun BudgetCycle.toShortPeriodLabel(): String =
    "${start.monthValue}. ${start.dayOfMonth} ~ ${lastDay.monthValue}. ${lastDay.dayOfMonth}"

/**
 * 통계 막대용 두 줄 표기 — "8.25" / "~9.24".
 *
 * 막대 하나에 배정되는 폭이 약 50dp라 한 줄로는 잘린다. 연도는 헤더가 보여주므로 뺀다.
 */
fun BudgetCycle.toBarLabel(): String =
    "${start.monthValue}.${start.dayOfMonth}\n~${lastDay.monthValue}.${lastDay.dayOfMonth}"

/** "26. 8. 25" — 두 자리 연도 + 월 + 일 */
private fun java.time.LocalDate.dateText(): String = "${year % 100}. $monthValue. $dayOfMonth"

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
