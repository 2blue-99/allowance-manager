package com.allowance.manager.core.domain.model

/**
 * 가계부 위젯용 이번 달(수급일 사이클) 집계.
 *
 * @param expense 이번 달 지출 합계 (가계부 집계 = BUDGET + LEDGER_ONLY)
 * @param income 이번 달 수입 합계 (가계부 집계 = BUDGET + LEDGER_ONLY)
 */
data class MonthlyLedger(
    val expense: Long,
    val income: Long,
)
