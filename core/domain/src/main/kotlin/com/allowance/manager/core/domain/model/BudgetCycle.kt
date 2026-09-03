package com.allowance.manager.core.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 월급일 기준 한 사이클 — 받은 날(포함) ~ 다음 받는 날(미포함).
 * payday: 1~31 = 해당 일, 0 = 말일. 해당 월에 없는 날짜(예: 31 → 2월)는 말일로 clamp.
 *
 * 경계는 [PaydayRule] 이력에서 나온다. **시작은 이력에 저장된 날짜를 그대로 쓰고**(사용자가
 * 캘린더에서 찍은 사실이므로 보정하지 않는다), 끝은 규칙일을 주말·공휴일 보정해 계산하되
 * 다음 이력이 더 이르면 거기서 끊는다 — 그래서 사이클 사이에 빈틈도 겹침도 생기지 않는다.
 *
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

        /**
         * [after]에 받은 다음, 그다음 지급일.
         *
         * [after]가 속한 달도 후보에 넣는다 — 1일에 받고 규칙을 25일로 바꾸면 24일 뒤인
         * 그달 25일이 다음 지급이다. 다만 [MIN_CYCLE_DAYS]일 이내로 붙는 후보는 **같은 지급이
         * 영업일 보정으로 앞당겨진 것**으로 보고 건너뛴다. (8/22에 받았고 규칙이 25일이면
         * 8/25가 아니라 9/25가 다음)
         */
        fun nextPayDateAfter(after: LocalDate, payday: Int, holidays: Holidays = Holidays.EMPTY): LocalDate {
            var ym = YearMonth.from(after)
            repeat(MAX_MONTH_PROBE) {
                val candidate = payDate(ym, payday, holidays)
                if (candidate.isAfter(after) && ChronoUnit.DAYS.between(after, candidate) >= MIN_CYCLE_DAYS) {
                    return candidate
                }
                ym = ym.plusMonths(1)
            }
            return after.plusDays(1)
        }

        /**
         * [before] 직전 지급일. [nextPayDateAfter]의 반대 방향.
         *
         * 이력의 첫 줄보다 과거를 물었을 때 그 규칙으로 거슬러 올라가는 데 쓴다.
         * 붙어 있는 후보를 건너뛰는 기준([MIN_CYCLE_DAYS])은 전진과 같다.
         */
        fun previousPayDateBefore(before: LocalDate, payday: Int, holidays: Holidays = Holidays.EMPTY): LocalDate {
            var ym = YearMonth.from(before)
            repeat(MAX_MONTH_PROBE) {
                val candidate = payDate(ym, payday, holidays)
                if (candidate.isBefore(before) && ChronoUnit.DAYS.between(candidate, before) >= MIN_CYCLE_DAYS) {
                    return candidate
                }
                ym = ym.minusMonths(1)
            }
            return before.minusDays(1)
        }

        /**
         * 이력 기반 사이클 — [today]가 속한 구간.
         *
         * [rules]는 오래된 → 최신 순. 비어 있으면 [fallbackPayday]로 규칙일 하나만 쓰던
         * 기존 계산([of])과 같은 결과를 준다(온보딩 이전, DB만 초기화된 개발 기기 등).
         *
         * 이력의 첫 줄보다 과거를 물으면 그 규칙으로 **거슬러 올라가** 사이클을 만든다.
         * (온보딩은 그 시점 사이클 한 줄만 심으므로, 역산이 없으면 과거 사이클이 아예 안 나온다)
         */
        fun of(
            rules: List<PaydayRule>,
            today: LocalDate = LocalDate.now(),
            holidays: Holidays = Holidays.EMPTY,
            fallbackPayday: Int = 0,
        ): BudgetCycle {
            if (rules.isEmpty()) return of(fallbackPayday, today, holidays)

            val sorted = rules.sortedBy { it.effectiveDate }

            // 첫 이력 이전 — 그 규칙으로 역산
            val first = sorted.first()
            if (today.isBefore(first.effectiveDate)) {
                var end = first.effectiveDate
                repeat(MAX_CYCLE_PROBE) {
                    val begin = previousPayDateBefore(end, first.payday, holidays)
                    if (!today.isBefore(begin)) return BudgetCycle(begin, end)
                    end = begin
                }
                return BudgetCycle(startBefore(today, end), end)
            }

            val startIndex = sorted.indexOfLast { !it.effectiveDate.isAfter(today) }.coerceAtLeast(0)

            var index = startIndex
            var start = sorted[index].effectiveDate
            repeat(MAX_CYCLE_PROBE) {
                val payday = sorted[index].payday
                val nextRuleDate = sorted.getOrNull(index + 1)?.effectiveDate
                // 규칙일로 계산한 다음 경계. 그 전에 새 이력이 있으면 거기서 끊는다.
                val byRule = nextPayDateAfter(start, payday, holidays)
                val end = if (nextRuleDate != null && nextRuleDate.isBefore(byRule)) nextRuleDate else byRule

                if (today.isBefore(end)) return BudgetCycle(start, endAfter(end, start))

                start = end
                if (nextRuleDate != null && !start.isBefore(nextRuleDate)) index++
            }
            // 이력이 심하게 오래됐을 때의 안전망 — 마지막 규칙으로 한 사이클을 만든다.
            val payday = sorted.last().payday
            return BudgetCycle(start, endAfter(nextPayDateAfter(start, payday, holidays), start))
        }

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

        /** 규칙일이 며칠이든 12개월 안에는 [after] 뒤 지급일이 나온다. */
        private const val MAX_MONTH_PROBE = 13

        /**
         * 사이클로 인정하는 최소 일수. 이보다 짧게 붙는 지급일 후보는 건너뛴다.
         *
         * 두 경우를 함께 막는다.
         * - 같은 지급이 영업일 보정·조정으로 앞당겨진 것 (8/22에 받고 규칙 25일 → 8/25 아님)
         * - 규칙을 바꿨을 때 **이미 지나간 새 규칙일** (8/1에 받고 10일로 변경 → 8/10 아님)
         *
         * 사이클은 월 주기라 실제 길이가 28일 밑으로 내려가지 않으므로 이 값과 충돌하지 않는다.
         */
        private const val MIN_CYCLE_DAYS = 10L

        /** 이력 시작부터 오늘까지 훑는 사이클 수 상한 — 무한 루프 방지용. */
        private const val MAX_CYCLE_PROBE = 600

        // 경계 단조성 가드 — 영업일 보정으로 두 경계가 같은 날이 될 수 있다.
        // 길이 0 사이클은 endMillis < startMillis 가 되어 집계가 통째로 비므로 최소 1일을 보장한다.
        private fun endAfter(candidate: LocalDate, start: LocalDate): LocalDate =
            if (candidate.isAfter(start)) candidate else start.plusDays(1)

        private fun startBefore(candidate: LocalDate, endExclusive: LocalDate): LocalDate =
            if (candidate.isBefore(endExclusive)) candidate else endExclusive.minusDays(1)
    }
}
