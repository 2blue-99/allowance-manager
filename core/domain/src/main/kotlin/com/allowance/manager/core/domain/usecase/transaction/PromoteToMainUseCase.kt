package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.model.MaskedAccount
import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import com.allowance.manager.core.domain.usecase.account.promoteExisting
import javax.inject.Inject

/**
 * 비메인 내역을 메인으로 승격 (내역 시트 '메인 계좌로 등록' ON → 저장).
 * - 계좌번호가 있으면: 번호 계좌로 등록하고 같은 번호의 기존 내역에 소급 적용
 * - 계좌번호가 없으면(카드 등): 출처(앱) 계좌로 등록하고 같은 출처의 계좌번호 없는 내역에 소급 적용
 * - 같은 계좌가 이미 있으면(꺼져 있어도) 새로 만들지 않고 그 계좌를 켜서 재사용 — 중복 등록 방지
 */
class PromoteToMainUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(packageName: String, bankName: String, accountPattern: String?): Long {
        // 알림에서 온 값은 "941602-**-***318" 형태 → 구분자를 걷어내고 숫자·마스킹만 저장. 없으면 출처 계좌.
        val pattern = accountPattern?.let { MaskedAccount.normalize(it) }?.takeIf { it.isNotBlank() }
        val existing = accountRepository.getAll().firstOrNull { it.matchesTransaction(pattern, packageName) }

        val account = if (existing != null) {
            if (!existing.enabled) accountRepository.update(existing.copy(enabled = true))
            existing
        } else {
            val draft = Account(
                packageName = packageName,
                bankName = bankName,
                accountPattern = pattern.orEmpty(),
                enabled = true,
            )
            draft.copy(id = accountRepository.add(draft))
        }
        transactionRepository.promoteExisting(account)
        return account.id
    }
}
