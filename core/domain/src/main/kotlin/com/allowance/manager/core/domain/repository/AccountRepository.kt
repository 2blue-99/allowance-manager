package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    suspend fun getEnabled(): List<Account>
    suspend fun add(account: Account): Long
    suspend fun update(account: Account)
    suspend fun delete(account: Account)
}
