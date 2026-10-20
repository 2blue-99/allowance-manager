package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 메인 해제 (내역 시트 '메인 계좌로 등록' OFF → 저장). 등록이 계좌 단위이듯 해제도 계좌 단위.
 * - 그 계좌를 끈다(계좌관리 스위치 OFF와 동일 — 목록에 남아 다시 켤 수 있음)
 * - 그 계좌의 내역 전부 비메인·EXCLUDED → 메인 필터에서 빠지고 전체에서만 보이며, 예산 집계에서 제외
 * - 계좌가 이미 지워진 내역(옛 데이터의 끊긴 accountId)도 내역만 해제한다
 */
class DemoteFromMainUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(accountId: Long) {
        accountRepository.getById(accountId)
            ?.takeIf { it.enabled }
            ?.let { accountRepository.update(it.copy(enabled = false)) }
        transactionRepository.demoteFromMain(accountId)
    }
}
