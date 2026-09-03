package com.allowance.manager.core.domain.model

import java.time.LocalDate

/**
 * 수급일 사이클 한 개(저장 행) — 기간 + 그 사이클의 예산 + 생성 규칙일.
 *
 * 화면 표시는 순수 기간 모델([BudgetCycle], [period])을 그대로 쓴다.
 * 최신 행의 [payday]가 곧 현재 규칙(단일 소스)이다.
 */
data class Cycle(
    val start: LocalDate,          // 받은 날 (포함)
    val endExclusive: LocalDate,   // 다음 받는 날 (미포함). 마지막 행만 '예정'
    val budget: Long,              // 이 사이클 예산 (생성 시 직전 행 값 이월)
    val payday: Int,               // 규칙일 1~31, 0 = 말일
) {
    /** 기간만 필요한 화면·계산용 뷰 */
    val period: BudgetCycle get() = BudgetCycle(start, endExclusive)
}
