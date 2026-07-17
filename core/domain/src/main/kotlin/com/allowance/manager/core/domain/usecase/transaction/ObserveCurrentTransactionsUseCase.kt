package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * 이번달(현재 사이클) 입출금 내역을 최신순으로 관찰.
 */
class ObserveCurrentTransactionsUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
    private val transactionRepository: TransactionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Transaction>> =
        dataStoreRepository.getPayday().flatMapLatest { payday ->
            val cycle = BudgetCycle.of(payday)
            transactionRepository.observeBetween(cycle.startMillis(), cycle.endMillis())
        }
}
