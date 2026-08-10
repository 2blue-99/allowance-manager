package com.allowance.manager.core.domain.model

enum class TransactionType { EXPENSE, INCOME, TRANSFER }

/**
 * 예산 반영 상태 — 내역 한 건이 어느 합계에 들어가는지. (구 isHidden 대체)
 *
 * - BUDGET      : 남은 금액(예산) + 가계부 합계 모두 포함
 * - LEDGER_ONLY : 가계부 합계·통계만 포함, 예산(남은 금액) 제외
 * - EXCLUDED    : 전부 제외 — 전체 필터에서만 노출 (미등록·사용자 제외)
 */
enum class TxScope { BUDGET, LEDGER_ONLY, EXCLUDED }

/**
 * 입출금 내역 한 건 (가계부).
 *
 * - amount: 지출=양수, 취소/환불=음수(type=EXPENSE), 입금=양수(type=INCOME)
 * - accountId: 매칭된 등록계좌 id. null 이면 비메인
 * - scope: 예산 반영 상태(위 TxScope). 저장 시 등록계좌·수동=BUDGET, 미등록=EXCLUDED
 */
data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val balance: Long? = null,
    val packageName: String,
    val sourceName: String,            // 출처(은행/카드/앱). 자동감지=패키지→은행맵, 없으면 알림 제목. 읽기전용
    val merchant: String? = null,      // 사용처(가게). 사용자가 기록. null이면 리스트에 sourceName(출처)로 대체 노출
    val extractedAccount: String? = null,
    val accountId: Long? = null,
    val category: TransactionCategory? = null,
    val memo: String? = null,
    val scope: TxScope = TxScope.BUDGET,  // 예산 반영 상태 (구 isHidden)
    val isManual: Boolean = false,  // 사용자가 직접 추가한 내역
    val createdAt: Long,            // epoch ms
) {
    val isMain: Boolean get() = accountId != null

    /** 남은 금액(예산) 집계 대상 */
    val inBudget: Boolean get() = scope == TxScope.BUDGET

    /** 가계부 합계·통계 집계 대상 (EXCLUDED만 제외) */
    val inLedger: Boolean get() = scope != TxScope.EXCLUDED

    /**
     * 사용자가 예산에서 뺀 항목(회색 + '제외' 표시). 미등록(계좌 없음·자동감지)은 대상 아님.
     * 스와이프 제외/복원 토글 판정용.
     */
    val isHidden: Boolean get() = scope == TxScope.EXCLUDED && (accountId != null || isManual)
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
