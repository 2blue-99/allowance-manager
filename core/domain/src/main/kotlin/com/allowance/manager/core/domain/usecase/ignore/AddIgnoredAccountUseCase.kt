package com.allowance.manager.core.domain.usecase.ignore

import com.allowance.manager.core.domain.model.IgnoredAccount
import com.allowance.manager.core.domain.model.MaskedAccount
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.repository.IgnoredAccountRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 내역을 기준으로 무시 계좌 등록 + 같은 기준의 과거 내역 전 기간 삭제(소급).
 * - 계좌번호 있으면: 그 마스킹 계좌번호만 무시
 * - 계좌번호 없으면: 출처(앱) 전체 무시
 */
class AddIgnoredAccountUseCase @Inject constructor(
    private val ignoredAccountRepository: IgnoredAccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(tx: Transaction) {
        // 알림 계좌는 "941602-**-***318" 형태 → 구분자 걷어내고 숫자·마스킹만 저장. 없으면 출처 무시.
        val pattern = tx.extractedAccount
            ?.let { MaskedAccount.normalize(it) }
            ?.takeIf { it.isNotBlank() }
        ignoredAccountRepository.add(
            IgnoredAccount(
                packageName = tx.packageName,
                sourceName = tx.sourceName,
                accountPattern = pattern.orEmpty(),
                createdAt = System.currentTimeMillis(),
            ),
        )
        transactionRepository.deleteMatchingForIgnore(pattern, tx.packageName)
    }
}
