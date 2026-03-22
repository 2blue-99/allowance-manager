package com.allowance.manager.core.data.repository

import com.allowance.manager.core.datastore.PreferencesDataSource
import com.allowance.manager.core.domain.repository.BalanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
) : BalanceRepository {

    override fun getBalance(): Flow<Long> =
        preferencesDataSource.getBalance()

    override suspend fun setBalance(amount: Long) {
        preferencesDataSource.setBalance(amount)
    }
}
