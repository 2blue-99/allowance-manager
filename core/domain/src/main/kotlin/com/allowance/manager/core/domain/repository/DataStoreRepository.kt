package com.allowance.manager.core.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DataStoreRepository {
    fun getMonthAllowance(): Flow<Long>
    suspend fun setMonthAllowance(amount: Long)

    fun getNexPayDay(): Flow<String>
    suspend fun setNextPayday(date: String)
}
