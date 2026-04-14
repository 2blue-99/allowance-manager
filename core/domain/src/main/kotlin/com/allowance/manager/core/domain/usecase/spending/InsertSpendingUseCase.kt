package com.allowance.manager.core.domain.usecase.spending

import com.allowance.manager.core.domain.model.Spending
import com.allowance.manager.core.domain.repository.SpendingRepository
import javax.inject.Inject

class InsertSpendingUseCase @Inject constructor(
    private val spendingRepository: SpendingRepository,
) {
    suspend operator fun invoke(spending: Spending) {
        spendingRepository.insertSpending(spending)
    }
}
