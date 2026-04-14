package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.Spending
import kotlinx.coroutines.flow.Flow

interface SpendingRepository {
    suspend fun insertSpending(spending: Spending)
    fun getAllSpendings(): Flow<List<Spending>>
}
