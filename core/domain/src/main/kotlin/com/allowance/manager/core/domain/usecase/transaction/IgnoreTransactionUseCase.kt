package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

class IgnoreTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(id: Long, ignored: Boolean) =
        transactionRepository.setIgnored(id, ignored)
}
