package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/** 내역 숨김 토글 (합계 제외/복원, 내역은 보관). 무시(IgnoredAccount)와 다름. */
class HideTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(id: Long, hidden: Boolean) =
        transactionRepository.setHidden(id, hidden)
}
