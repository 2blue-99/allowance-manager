package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.BudgetStatus
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 이번달 예산 현황(예산·지출·남은/초과)을 관찰.
 * 수급일로 현재 사이클을 계산해 그 범위의 지출 합계를 구독.
 */
class ObserveBudgetStatusUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
    private val transactionRepository: TransactionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<BudgetStatus> =
        combine(
            dataStoreRepository.getMonthlyBudget(),
            dataStoreRepository.getPayday(),
        ) { budget, payday -> budget to payday }
            .flatMapLatest { (budget, payday) ->
                val cycle = BudgetCycle.of(payday)
                transactionRepository
                    .observeSpentBetween(cycle.startMillis(), cycle.endMillis())
                    .map { spent -> BudgetStatus(budget = budget, spent = spent, cycle = cycle) }
            }
}
