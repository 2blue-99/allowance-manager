package com.allowance.manager.core.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 입출금 내역 (가계부 한 건).
 *
 * - amount: 지출=양수, 취소/환불=음수(type=EXPENSE), 입금=양수(type=INCOME)
 * - accountId: 매칭된 등록계좌 id. null 이면 비메인(예산 미반영)
 * - 이번달 지출 합계 = SUM(amount) WHERE type='EXPENSE' AND account_id NOT NULL AND is_ignored=0
 */
@Entity(tableName = "transactions")
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
    @ColumnInfo(name = "is_ignored")
    val isIgnored: Boolean = false,
    @ColumnInfo(name = "is_manual")
    val isManual: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
