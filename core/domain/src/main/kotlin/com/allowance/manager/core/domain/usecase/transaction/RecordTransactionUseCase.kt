package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.MaskedAccount
import com.allowance.manager.core.domain.model.ParsedTransaction
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.IgnoredAccountRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 알림 파싱 결과를 가계부에 저장.
 * - 무시 계좌(IgnoredAccount)에 매칭되면 저장하지 않고 드롭 → null 반환.
 * - 알림에서 뽑은 마스킹 계좌가 등록된 enabled 계좌와 자리별로 일치하면 메인(accountId)으로 매칭.
 * - 비메인 + DB에 처음 등장한 계좌/출처면 "새 계좌 감지" 배지(전체 칩 빨간 점)를 켠다.
 */
class RecordTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val ignoredAccountRepository: IgnoredAccountRepository,
    private val dataStoreRepository: DataStoreRepository,
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

        // 비메인(미등록)이고, 이 계좌/출처의 기존 내역이 하나도 없으면 = 처음 보는 새 계좌 → 배지 ON
        if (accountId == null) {
            val pattern = parsed.extractedAccount
                ?.let { MaskedAccount.normalize(it) }
                ?.takeIf { it.isNotBlank() }
            val priorCount = transactionRepository.countMatchingForIgnore(pattern, parsed.packageName)
            if (priorCount == 0) dataStoreRepository.setHomeNewAccountBadge(true)
        }

        return transactionRepository.record(
            Transaction(
                type = parsed.type,
                amount = parsed.amount,
                balance = parsed.balance,
                packageName = parsed.packageName,
                sourceName = parsed.sourceName,
                extractedAccount = parsed.extractedAccount,
                accountId = accountId,
                // 미등록 계좌 = 제외(EXCLUDED). 등록계좌 매칭 시 수입은 예산 미반영(가계부만),
                // 그 외(지출·이체)는 예산 반영(BUDGET). 이후 사용자·승격으로 조정.
                scope = when {
                    accountId == null -> TxScope.EXCLUDED
                    parsed.type == TransactionType.INCOME -> TxScope.LEDGER_ONLY
                    else -> TxScope.BUDGET
                },
                createdAt = System.currentTimeMillis(),
            )
        )
    }
}
