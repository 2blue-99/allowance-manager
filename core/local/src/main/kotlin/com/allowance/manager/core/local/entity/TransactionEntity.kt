package com.allowance.manager.core.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 입출금 내역 (가계부 한 건).
 *
 * - amount: 지출=양수, 취소/환불=음수(type=EXPENSE), 입금=양수(type=INCOME)
 * - accountId: 매칭된 등록계좌 id. null 이면 비메인
 * - scope: 예산 반영 상태 "BUDGET" | "LEDGER_ONLY" | "EXCLUDED" (구 is_hidden 대체)
 * - 남은 금액 예산 지출 = SUM(amount) WHERE type='EXPENSE' AND scope='BUDGET'
 */
@Entity(
    tableName = "transactions",
    // 월별·사이클 조회가 created_at 범위 필터에 크게 의존 → 인덱스로 스캔 비용 절감
    indices = [Index(value = ["created_at"])],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,                       // "EXPENSE" | "INCOME"
    val amount: Long,
    val balance: Long?,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "source_name")
    val sourceName: String,
    @ColumnInfo(name = "extracted_account")
    val extractedAccount: String?,          // 알림에서 파싱한 계좌 식별자 (매칭·승격용)
    @ColumnInfo(name = "account_id")
    val accountId: Long?,                   // null = 비메인
    val category: String?,                  // TransactionCategory.name, null = 미분류
    val memo: String?,
    val scope: String = "BUDGET",           // TxScope: "BUDGET" | "LEDGER_ONLY" | "EXCLUDED" (구 is_hidden)
    @ColumnInfo(name = "is_manual")
    val isManual: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
