package com.allowance.manager.core.domain.model

/**
 * 가계부 위젯용 이번 달(달력 월: 1일~말일) 집계.
 * 예산·홈(수급일 사이클)과 달리 달력 월 기준이다.
 *
 * @param expense 이번 달 지출 합계 (메인·숨김아님)
 * @param income 이번 달 수입 합계 (메인·숨김아님)
 */
data class MonthlyLedger(
    val expense: Long,
    val income: Long,
)
