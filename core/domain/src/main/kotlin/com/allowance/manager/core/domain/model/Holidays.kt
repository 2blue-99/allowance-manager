package com.allowance.manager.core.domain.model

import java.time.LocalDate

/**
 * 급여 지급일 보정에 쓰는 비영업일 목록 — 날짜와 이름(예: "추석")을 함께 들고 있다.
 *
 * 이름은 사용자에게 보여주는 데 쓴다("8월 31일은 추석이에요"). 날짜만 필요한 곳에서는
 * `date in holidays` 로 그냥 쓸 수 있게 [contains]를 뚫어뒀다.
 *
 * @param version 원격 데이터 버전. **0이면 내장 폴백**(Remote Config 미수신) — 디버그 화면에서 구분용.
 */
data class Holidays(
    val byDate: Map<LocalDate, String> = emptyMap(),
    val version: Int = 0,
) {
    operator fun contains(date: LocalDate): Boolean = date in byDate

    /** 그날의 공휴일 이름. 공휴일이 아니면 null */
    fun nameOf(date: LocalDate): String? = byDate[date]

    val count: Int get() = byDate.size

    companion object {
        val EMPTY = Holidays()
    }
}
