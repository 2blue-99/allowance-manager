package com.allowance.manager.core.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 월별 용돈(월 예산) 이력. effective-dated.
 *
 * - effectiveMonth: "yyyy-MM" — 이 달부터 적용. (문자열 정렬 = 시간 정렬)
 * - 용돈 변경 시 effectiveMonth = 이번 달로 upsert (같은 달 재변경은 그 행만 갱신).
 * - 특정 달 M의 용돈 = effectiveMonth <= M 중 최신 행 (안 바꾼 달은 직전 값 이월).
 */
@Entity(tableName = "budget_history")
data class BudgetEntity(
    @PrimaryKey
    @ColumnInfo(name = "effective_month")
    val effectiveMonth: String,
    val amount: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
