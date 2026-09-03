package com.allowance.manager.core.local.dao

/** 사이클별 합계 한 줄 — cycle = 사이클 시작일 "yyyy-MM-dd" */
data class CycleTotalRow(
    val cycle: String,
    val total: Long,
)
