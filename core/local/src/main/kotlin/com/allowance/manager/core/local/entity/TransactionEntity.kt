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
 * - cycleStart: 이 거래가 속한 사이클의 시작일 "yyyy-MM-dd". 사이클 경계는 사용자마다·달마다
 *   달라 SQL이 스스로 계산할 수 없으므로 저장 시점에 박아둔다. 홈·월별·통계가 모두 이 값으로 묶는다.
 * - 남은 금액 예산 지출 = SUM(amount) WHERE type='EXPENSE' AND scope='BUDGET'
 */
@Entity(
    tableName = "transactions",
    indices = [
        // 리스트 정렬·범위 필터
        Index(value = ["created_at"]),
        // 사이클별 조회
        Index(value = ["cycle_start"]),
        // 예산 집계 (type='EXPENSE' AND scope='BUDGET')
        Index(value = ["cycle_start", "type", "scope"]),
    ],
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
    val merchant: String? = null,           // 사용처(가게). null = 미기록 → 리스트에서 sourceName로 대체
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
    /** 속한 사이클의 시작일 "yyyy-MM-dd" — 경계가 바뀌면 재계산해 다시 채운다. */
    @ColumnInfo(name = "cycle_start")
    val cycleStart: String,
)
