package com.allowance.manager.core.data.repository

import com.allowance.manager.core.datastore.PreferencesDataSource
import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
) : DataStoreRepository {

    override fun getDailyAllowance(): Flow<Long> =
        preferencesDataSource.getDailyAllowance()

    override suspend fun setDailyAllowance(amount: Long) {
        preferencesDataSource.setDailyAllowance(amount)
    }

    override suspend fun changeDailyAllowance(delta: Long) {
        preferencesDataSource.changeDailyAllowance(delta)
    }

    override fun getMonthAllowance(): Flow<Long> =
        preferencesDataSource.getMonthAllowance()

    override suspend fun setMonthAllowance(amount: Long) {
        preferencesDataSource.setMonthAllowance(amount)
    }

    override fun getNextPayday(): Flow<String> =
        preferencesDataSource.getNextPayday()

    override suspend fun setNextPayday(date: String) {
        preferencesDataSource.setNextPayday(date)
    }
}
