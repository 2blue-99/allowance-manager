package com.allowance.manager.core.domain.usecase.ignore

import com.allowance.manager.core.domain.model.MaskedAccount
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/** 무시 시 삭제될 기존 내역 건수 (다이얼로그 "기존 내역 {n}건"). */
class CountIgnorableTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(tx: Transaction): Int {
        val pattern = tx.extractedAccount
            ?.let { MaskedAccount.normalize(it) }
            ?.takeIf { it.isNotBlank() }
        return transactionRepository.countMatchingForIgnore(pattern, tx.packageName)
    }
}
