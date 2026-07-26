package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 바텀시트 '저장' — 사용자가 수정한 항목(메모·분류)을 DB에 upsert.
 * 메모는 공백만 있으면 null 로 정리한다.
 */
class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(id: Long, memo: String, category: TransactionCategory?) {
        val tx = transactionRepository.getById(id) ?: return
        transactionRepository.update(
            tx.copy(
                memo = memo.trim().ifBlank { null },
                category = category,
            )
        )
    }
}
