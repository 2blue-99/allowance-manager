package com.allowance.manager.core.domain.model

import java.time.YearMonth

/**
 * 달별 월급일 지정("이번 달 월급일 조정") 저장 형식.
 *
 * 규칙일(payday)과 별개로 "그 달만 이 날짜에 받았다"를 기록한다. 값이 있으면 [BudgetCycle.payDate]가
 * 영업일 보정 없이 그 날짜를 그대로 쓴다.
 *
 * ⚠️ 지난 달 값도 남겨야 한다. 조정한 달이 지나자마자 값을 버리면 그 달 사이클 경계가 규칙일로 되돌아가,
 * 조정일과 규칙일 사이 거래가 어느 사이클에도 안 잡히거나 두 번 잡힌다. 그래서 최근 [MAX_MONTHS]개월을 보존한다.
 *
 * 저장 문자열: `2026-08:21,2026-09:23`
 */
object PaydayOverrides {

    /** 보존할 최대 개월 수 — 오래된 달부터 버린다. */
    const val MAX_MONTHS = 12

    fun parse(raw: String): Map<YearMonth, Int> =
        raw.split(',')
            .mapNotNull { entry ->
                val (ymPart, dayPart) = entry.trim().split(':').let {
                    if (it.size != 2) return@mapNotNull null else it[0] to it[1]
                }
                val ym = runCatching { YearMonth.parse(ymPart.trim()) }.getOrNull() ?: return@mapNotNull null
                val day = dayPart.trim().toIntOrNull()?.takeIf { it in 1..31 } ?: return@mapNotNull null
                ym to day
            }
            .toMap()

    fun format(overrides: Map<YearMonth, Int>): String =
        overrides.entries
            .sortedBy { it.key }
            .joinToString(",") { "${it.key}:${it.value}" }

    /**
     * [ym] 달의 지정값을 바꾼 새 맵. [day]가 null이면 지정 해제(= 규칙일로 계산).
     * 결과는 최근 [MAX_MONTHS]개월만 남긴다.
     */
    fun put(current: Map<YearMonth, Int>, ym: YearMonth, day: Int?): Map<YearMonth, Int> {
        val updated = current.toMutableMap()
        if (day == null) updated.remove(ym) else updated[ym] = day.coerceIn(1, 31)
        if (updated.size <= MAX_MONTHS) return updated
        return updated.entries
            .sortedByDescending { it.key }
            .take(MAX_MONTHS)
            .associate { it.key to it.value }
    }
}
