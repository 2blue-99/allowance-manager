package com.allowance.manager.core.domain.usecase.store

import com.allowance.manager.core.domain.repository.DataStoreRepository
import javax.inject.Inject

class SetDailyAllowanceUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(amount: Long) {
        dataStoreRepository.setDailyAllowance(amount)
    }
}
