package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetStatus
import com.allowance.manager.core.domain.repository.CycleRepository
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
    private val cycleRepository: CycleRepository,
    private val transactionRepository: TransactionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<BudgetStatus> =
        cycleRepository.observeCycleAt().flatMapLatest { cycle ->
            val period = cycle.period
            combine(
                transactionRepository.observeBudgetSpentBetween(period.startMillis(), period.endMillis()),
                transactionRepository.observeBudgetIncomeBetween(period.startMillis(), period.endMillis()),
            ) { spent, income ->
                BudgetStatus(budget = cycle.budget, spent = spent, income = income, cycle = period)
            }
        }
}
