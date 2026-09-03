package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetStatus
import com.allowance.manager.core.domain.repository.BudgetRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * 현재 사이클의 예산 현황(예산·지출·남은/초과)을 관찰.
 *
 * 예산·지출·수입이 **모두 같은 사이클**을 기준으로 나온다. 홈·위젯·상태바가 이 하나를 공유하므로
 * 여기서 나온 값을 화면에서 다시 계산하지 말 것 — 재계산하면 값이 어긋난다.
 */
class ObserveBudgetStatusUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val observeCycleUseCase: ObserveCycleUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<BudgetStatus> =
        observeCycleUseCase().flatMapLatest { cycle ->
            combine(
                budgetRepository.observeBudgetForCycle(cycle.start),
                transactionRepository.observeBudgetSpentInCycle(cycle.start),
                transactionRepository.observeBudgetIncomeInCycle(cycle.start),
            ) { budget, spent, income ->
                BudgetStatus(budget = budget, spent = spent, income = income, cycle = cycle)
            }
        }
}
