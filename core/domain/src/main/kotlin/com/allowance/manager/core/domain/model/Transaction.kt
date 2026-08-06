package com.allowance.manager.core.domain.model

enum class TransactionType { EXPENSE, INCOME, TRANSFER }

/**
 * 입출금 내역 한 건 (가계부).
 *
 * - amount: 지출=양수, 취소/환불=음수(type=EXPENSE), 입금=양수(type=INCOME)
 * - accountId: 매칭된 등록계좌 id. null 이면 비메인(예산 미반영)
 */
data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val balance: Long? = null,
    val packageName: String,
    val sourceName: String,
    val extractedAccount: String? = null,
    val accountId: Long? = null,
    val category: TransactionCategory? = null,
    val memo: String? = null,
    val isHidden: Boolean = false,  // 숨김 (합계 제외, 복원 가능)
    val isManual: Boolean = false,  // 사용자가 직접 추가한 내역
    val createdAt: Long,            // epoch ms
) {
    val isMain: Boolean get() = accountId != null

    /** 예산·통계 집계 대상 여부(메인 계좌 매칭 또는 수동 입력). 숨김 여부는 별도. */
    val isCounted: Boolean get() = accountId != null || isManual
}

/**
 * 알림 파싱 결과 → 저장 요청용 입력 모델.
 * rawText 는 등록 계좌 패턴 매칭에 사용.
 */
data class ParsedTransaction(
    val type: TransactionType,
    val amount: Long,               // 부호 포함 (환불 음수)
    val balance: Long?,
    val packageName: String,
    val sourceName: String,
    val extractedAccount: String?,
    val rawText: String,
)
