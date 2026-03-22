package com.allowance.manager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        val BUDGET = longPreferencesKey("budget")
        val REMAINING_BALANCE = longPreferencesKey("remaining_balance")
        val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
    }

    fun getBalance(): Flow<Long> = get(REMAINING_BALANCE, 0L)

    /**
     * 기존 값에 Amount 를 더함
     */
    suspend fun setBalance(amount: Long) {
        val resultBalance = getBalance().first() + amount
        set(REMAINING_BALANCE, resultBalance)
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
