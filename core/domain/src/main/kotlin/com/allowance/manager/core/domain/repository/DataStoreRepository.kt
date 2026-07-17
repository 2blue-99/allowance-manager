package com.allowance.manager.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface DataStoreRepository {
    fun getDailyAllowance(): Flow<Long>
    suspend fun setDailyAllowance(amount: Long)
    suspend fun changeDailyAllowance(delta: Long)

    fun getMonthAllowance(): Flow<Long>
    suspend fun setMonthAllowance(amount: Long)

    fun getNextPayday(): Flow<String>
    suspend fun setNextPayday(date: String)
}
