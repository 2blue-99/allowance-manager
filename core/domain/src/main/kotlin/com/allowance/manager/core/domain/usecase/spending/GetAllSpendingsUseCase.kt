package com.allowance.manager.core.domain.usecase.spending

import com.allowance.manager.core.domain.model.Spending
import com.allowance.manager.core.domain.repository.SpendingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllSpendingsUseCase @Inject constructor(
    private val spendingRepository: SpendingRepository,
) {
    operator fun invoke(): Flow<List<Spending>> = spendingRepository.getAllSpendings()
}
