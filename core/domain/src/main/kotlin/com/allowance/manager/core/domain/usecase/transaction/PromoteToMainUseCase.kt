package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 비메인 내역을 메인으로 승격.
 * accountPattern 을 새 메인 계좌로 등록하고, 같은 패턴의 기존 내역에 소급 적용.
 */
class PromoteToMainUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(packageName: String, bankName: String, accountPattern: String): Long {
        val accountId = accountRepository.add(
            Account(
                packageName = packageName,
                bankName = bankName,
                accountPattern = accountPattern,
                enabled = true,
            )
        )
        transactionRepository.promoteToMain(accountPattern, accountId)
        return accountId
    }
}
