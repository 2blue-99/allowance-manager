package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * 이번달(현재 사이클) 입출금 내역을 최신순으로 관찰.
 * 사이클은 홈 예산 카드와 같은 소스(`cycles` 행)를 쓴다 — 카드와 리스트 기간이 어긋나지 않는다.
 */
class ObserveCurrentTransactionsUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val transactionRepository: TransactionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Transaction>> =
        cycleRepository.observeCycleAt().flatMapLatest { cycle ->
            val period = cycle.period
            transactionRepository.observeBetween(period.startMillis(), period.endMillis())
        }
}
