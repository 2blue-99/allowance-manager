package com.allowance.manager.core.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 월급일 이력. effective-dated — `budget_history`와 같은 이월 방식.
 *
 * 한 줄 = **"이 날짜에 월급을 받았고, 그 이후 규칙은 [payday]"**.
 *
 * - effectiveDate: "yyyy-MM-dd" — 이 날부터 새 사이클이 시작한다. (문자열 정렬 = 시간 정렬)
 * - payday: 1~31, 0 = 말일. 다음 사이클 경계를 계산할 규칙일.
 * - 특정 날짜 D의 규칙 = effectiveDate <= D 중 최신 행.
 *
 * 규칙일만 바꾸든, 받은 날만 옮기든, 둘 다 하든 전부 이 한 형태로 기록된다.
 * (그래서 "이번 달만 조정" 같은 별도 개념이 필요 없다)
 */
@Entity(tableName = "payday_history")
data class PaydayHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "effective_date")
    val effectiveDate: String,
    val payday: Int,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
