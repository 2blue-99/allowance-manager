package com.allowance.manager.core.domain.usecase.balance

import com.allowance.manager.core.domain.repository.BalanceRepository
import javax.inject.Inject

class SetBalanceUseCase @Inject constructor(
    private val balanceRepository: BalanceRepository,
) {
    suspend operator fun invoke(amount: Long) {
        balanceRepository.setBalance(amount)
    }
}
