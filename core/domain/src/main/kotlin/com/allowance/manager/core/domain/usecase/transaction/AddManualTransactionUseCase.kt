package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 사용자가 바텀시트에서 직접 추가하는 내역.
 * 수동 입력은 계좌 매칭이 없어도 예산·통계에 집계되도록 isManual=true 로 저장한다.
 */
class AddManualTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        type: TransactionType,
        amount: Long,
        sourceName: String,
        category: TransactionCategory?,
        memo: String,
    ): Long = transactionRepository.record(
        Transaction(
            type = type,
            amount = amount,
            balance = null,
            packageName = "",
            sourceName = sourceName.trim(),
            extractedAccount = null,
            accountId = null,
            category = category,
            memo = memo.trim().ifBlank { null },
            isManual = true,
            createdAt = System.currentTimeMillis(),
        )
    )
}
