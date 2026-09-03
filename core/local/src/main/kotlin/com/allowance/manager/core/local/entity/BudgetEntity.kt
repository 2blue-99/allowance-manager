package com.allowance.manager.core.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 용돈(월 예산) 이력. effective-dated.
 *
 * - effectiveCycle: "yyyy-MM-dd" — 이 사이클부터 적용. (문자열 정렬 = 시간 정렬)
 * - 용돈 변경 시 effectiveCycle = 현재 사이클 시작일로 upsert (같은 사이클 재변경은 그 행만 갱신).
 * - 특정 사이클 C의 용돈 = effectiveCycle <= C 중 최신 행 (안 바꾼 사이클은 직전 값 이월).
 *
 * 키가 달력 월이 아니라 사이클 시작일이라, 거래(`transactions.cycle_start`)·월급일 이력과
 * 같은 축으로 이어진다. "명목 월"을 계산할 일이 없다.
 */
@Entity(tableName = "budget_history")
data class BudgetEntity(
    @PrimaryKey
    @ColumnInfo(name = "effective_cycle")
    val effectiveCycle: String,
    val amount: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
