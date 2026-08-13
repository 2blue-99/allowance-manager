package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 사용자가 바텀시트에서 직접 추가하는 내역.
 * 수동 입력은 계좌 매칭이 없어도 예산·통계에 집계되도록 isManual=true 로 저장한다.
 * 예산 반영 상태(scope)는 시트에서 사용자가 고른 값을 그대로 저장한다.
 * (기본값은 시트가 유형에 따라 정함 — 수입은 '가계부만', 그 외는 예산 반영)
 */
class AddManualTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        type: TransactionType,
        amount: Long,
        merchant: String,
        category: TransactionCategory?,
        memo: String,
        scope: TxScope,
    ): Long = transactionRepository.record(
        Transaction(
            type = type,
            amount = amount,
            balance = null,
            packageName = "",
            // 직접추가는 출처(은행)가 없음 → sourceName 빈값, 사용처(merchant)에 입력값 저장
            sourceName = "",
            merchant = merchant.trim().ifBlank { null },
            extractedAccount = null,
            accountId = null,
            category = category,
            memo = memo.trim().ifBlank { null },
            scope = scope,
            isManual = true,
            createdAt = System.currentTimeMillis(),
        )
    )
}
