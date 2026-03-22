package com.allowance.manager.core.domain.usecase.balance

import com.allowance.manager.core.domain.repository.BalanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBalanceUseCase @Inject constructor(
    private val balanceRepository: BalanceRepository,
) {
    operator fun invoke(): Flow<Long> = balanceRepository.getBalance()
}
