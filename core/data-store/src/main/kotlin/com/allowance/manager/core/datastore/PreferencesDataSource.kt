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
        val MONTH_ALLOWANCE = longPreferencesKey("month_allowance")
        val NEXT_PAYDAY = stringPreferencesKey("next_payday")
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
