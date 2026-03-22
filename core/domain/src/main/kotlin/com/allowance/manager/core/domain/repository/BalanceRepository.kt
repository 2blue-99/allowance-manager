package com.allowance.manager.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface BalanceRepository {
    fun getBalance(): Flow<Long>
    suspend fun setBalance(amount: Long)
}
