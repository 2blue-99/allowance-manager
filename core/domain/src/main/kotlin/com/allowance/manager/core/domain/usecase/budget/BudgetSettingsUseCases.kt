package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMonthlyBudgetUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<Long> = dataStoreRepository.getMonthlyBudget()
}

class SetMonthlyBudgetUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(amount: Long) = dataStoreRepository.setMonthlyBudget(amount)
}

class GetPaydayUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<Int> = dataStoreRepository.getPayday()
}

class SetPaydayUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(day: Int) = dataStoreRepository.setPayday(day)
}
