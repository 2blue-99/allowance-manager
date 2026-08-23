package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.BudgetStatus
import com.allowance.manager.core.domain.repository.BudgetRepository
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.YearMonth
import javax.inject.Inject

/**
 * 이번달 예산 현황(예산·지출·남은/초과)을 관찰.
 * 용돈은 이번 달 이력값, 지출은 수급일 사이클 범위의 합계를 구독.
 */
class ObserveBudgetStatusUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val transactionRepository: TransactionRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<BudgetStatus> =
        combine(
            budgetRepository.observeBudgetForMonth(YearMonth.now()),
            dataStoreRepository.getPayday(),
            dataStoreRepository.getPaydayOverrides(),
        ) { budget, payday, overrides -> Triple(budget, payday, overrides) }
            .flatMapLatest { (budget, payday, overrides) ->
                val cycle = BudgetCycle.of(
                    payday = payday,
                    holidays = remoteConfigRepository.getHolidays(),
                    overrides = overrides,
                )
                combine(
                    transactionRepository.observeBudgetSpentBetween(cycle.startMillis(), cycle.endMillis()),
                    transactionRepository.observeBudgetIncomeBetween(cycle.startMillis(), cycle.endMillis()),
                ) { spent, income -> BudgetStatus(budget = budget, spent = spent, income = income, cycle = cycle) }
            }
}
