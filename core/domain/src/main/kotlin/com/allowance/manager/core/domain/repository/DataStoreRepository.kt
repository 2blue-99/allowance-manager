package com.allowance.manager.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface DataStoreRepository {
    fun getMonthlyBudget(): Flow<Long>
    suspend fun setMonthlyBudget(amount: Long)

    fun getPayday(): Flow<Int>          // 1~31, 0 = 말일
    suspend fun setPayday(day: Int)

    fun getIntroShown(): Flow<Boolean>
    suspend fun setIntroShown(shown: Boolean)

    fun getOnboardingDone(): Flow<Boolean>
    suspend fun setOnboardingDone(done: Boolean)

    fun getStatusBarEnabled(): Flow<Boolean>
    suspend fun setStatusBarEnabled(enabled: Boolean)

    fun getShowMainOnly(): Flow<Boolean>
    suspend fun setShowMainOnly(mainOnly: Boolean)
}
