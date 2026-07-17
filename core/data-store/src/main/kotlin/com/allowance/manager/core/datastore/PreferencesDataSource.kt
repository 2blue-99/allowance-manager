package com.allowance.manager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        // 하루에 쓸 금액.
        // 홈에서 세팅하거나 00시가 지날 때 마다 Room을 조회해서 갱신함
        val DAILY_ALLOWANCE = longPreferencesKey("daily_allowance")
        val MONTH_ALLOWANCE = longPreferencesKey("month_allowance")
        val NEXT_PAYDAY = stringPreferencesKey("next_payday")
    }

    fun getDailyAllowance(): Flow<Long> = get(DAILY_ALLOWANCE, 0L)
    suspend fun setDailyAllowance(amount: Long) {
        set(DAILY_ALLOWANCE, amount)
    }
    suspend fun changeDailyAllowance(delta: Long) {
        dataStore.edit { it[DAILY_ALLOWANCE] = (it[DAILY_ALLOWANCE] ?: 0L) + delta }
    }

    fun getMonthAllowance(): Flow<Long> = get(MONTH_ALLOWANCE, 0L)
    suspend fun setMonthAllowance(amount: Long) {
        set(MONTH_ALLOWANCE, amount)
    }

    fun getNextPayday(): Flow<String> = get(NEXT_PAYDAY, "25")
    suspend fun setNextPayday(date: String) {
        set(NEXT_PAYDAY, date)
    }

    private fun <T> get(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        dataStore.data.map { it[key] ?: defaultValue }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private suspend fun <T> remove(key: Preferences.Key<T>) {
        dataStore.edit { it.remove(key) }
    }
}
