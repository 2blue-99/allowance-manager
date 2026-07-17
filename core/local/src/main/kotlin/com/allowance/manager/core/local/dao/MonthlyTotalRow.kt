package com.allowance.manager.core.local.dao

/** 월별 지출 합계 집계 결과 (Room projection). ym 형식: "2026-07" */
data class MonthlyTotalRow(
    val ym: String,
    val total: Long,
)
