package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

class UpdateTransactionMemoUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(id: Long, memo: String) {
        val tx = transactionRepository.getById(id) ?: return
        transactionRepository.update(tx.copy(memo = memo.trim().ifBlank { null }))
    }
}
