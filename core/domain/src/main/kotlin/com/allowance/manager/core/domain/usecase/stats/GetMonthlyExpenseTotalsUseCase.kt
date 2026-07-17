package com.allowance.manager.core.domain.usecase.stats

import com.allowance.manager.core.domain.model.MonthlyExpense
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMonthlyExpenseTotalsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(): Flow<List<MonthlyExpense>> =
        transactionRepository.observeMonthlyExpenseTotals()
}
