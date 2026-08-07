package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 내역의 예산 반영 상태 지정 (BUDGET / LEDGER_ONLY / EXCLUDED).
 * 상세시트의 3분기 컨트롤에서 호출. 내역은 그대로 보관하고 집계 소속만 바꾼다.
 */
class SetTxScopeUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(id: Long, scope: TxScope) =
        transactionRepository.setScope(id, scope)
}
