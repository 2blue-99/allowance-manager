package com.allowance.manager.core.domain.usecase.store

import com.allowance.manager.core.domain.repository.DataStoreRepository
import java.time.LocalDate
import javax.inject.Inject

class SetNextPaydayUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(date: String) {
        dataStoreRepository.setNextPayday(date)
    }
}
