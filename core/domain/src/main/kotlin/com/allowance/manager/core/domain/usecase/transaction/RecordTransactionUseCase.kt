package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.ParsedTransaction
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 알림 파싱 결과를 가계부에 저장.
 * 알림에서 뽑은 마스킹 계좌가 등록된 enabled 계좌와 자리별로 일치하면 메인(accountId)으로 매칭.
 */
class RecordTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(parsed: ParsedTransaction): Long {
        // 번호 계좌는 계좌번호로, 출처 계좌는 앱(packageName)으로 매칭 (Account.matchesTransaction)
        val accountId = accountRepository.getEnabled()
            .firstOrNull { it.matchesTransaction(parsed.extractedAccount, parsed.packageName) }
            ?.id

        return transactionRepository.record(
            Transaction(
                type = parsed.type,
                amount = parsed.amount,
                balance = parsed.balance,
                packageName = parsed.packageName,
                sourceName = parsed.sourceName,
                extractedAccount = parsed.extractedAccount,
                accountId = accountId,
                createdAt = System.currentTimeMillis(),
            )
        )
    }
}
