package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.ParsedTransaction
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 알림 파싱 결과를 가계부에 저장.
 * 등록된 enabled 계좌 패턴이 알림 텍스트에 포함되면 메인(accountId)으로 매칭.
 */
class RecordTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(parsed: ParsedTransaction): Long {
        val accountId = accountRepository.getEnabled()
            .firstOrNull { parsed.rawText.contains(it.accountPattern) }
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
