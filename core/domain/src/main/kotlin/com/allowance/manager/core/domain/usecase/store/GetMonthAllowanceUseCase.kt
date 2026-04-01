package com.allowance.manager.core.domain.usecase.store

import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMonthAllowanceUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<Long> = dataStoreRepository.getMonthAllowance()
}
