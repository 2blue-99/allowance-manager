package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(id: Long) = transactionRepository.delete(id)
}
