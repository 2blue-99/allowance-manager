package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.ParsedTransaction
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.domain.repository.IgnoredAccountRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 알림 파싱 결과를 가계부에 저장.
 * - 무시 계좌(IgnoredAccount)에 매칭되면 저장하지 않고 드롭 → null 반환.
 * - 알림에서 뽑은 마스킹 계좌가 등록된 enabled 계좌와 자리별로 일치하면 메인(accountId)으로 매칭.
 */
class RecordTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val ignoredAccountRepository: IgnoredAccountRepository,
) {
    /** 저장된 내역 id, 무시 계좌 매칭으로 드롭되면 null */
    suspend operator fun invoke(parsed: ParsedTransaction): Long? {
        // 무시 계좌에 매칭되면 아예 기록하지 않음 (숨김과 다름)
        val ignored = ignoredAccountRepository.getAll()
            .any { it.matches(parsed.extractedAccount, parsed.packageName) }
        if (ignored) return null

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
