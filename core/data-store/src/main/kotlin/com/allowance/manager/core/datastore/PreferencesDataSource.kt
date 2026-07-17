package com.allowance.manager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        val MONTHLY_BUDGET = longPreferencesKey("monthly_budget")   // 월 예산 (0 = 미설정)
        val PAYDAY = intPreferencesKey("payday")                    // 수급일. 1~31, 0 = 말일
        val INTRO_SHOWN = booleanPreferencesKey("intro_shown")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val STATUS_BAR_ENABLED = booleanPreferencesKey("status_bar_enabled")
        val SHOW_MAIN_ONLY = booleanPreferencesKey("show_main_only")

        private const val DEFAULT_PAYDAY = 25
    }

    fun getMonthlyBudget(): Flow<Long> = get(MONTHLY_BUDGET, 0L)
    suspend fun setMonthlyBudget(amount: Long) = set(MONTHLY_BUDGET, amount)

    fun getPayday(): Flow<Int> = get(PAYDAY, DEFAULT_PAYDAY)
    suspend fun setPayday(day: Int) = set(PAYDAY, day)

    fun getIntroShown(): Flow<Boolean> = get(INTRO_SHOWN, false)
    suspend fun setIntroShown(shown: Boolean) = set(INTRO_SHOWN, shown)

    fun getOnboardingDone(): Flow<Boolean> = get(ONBOARDING_DONE, false)
    suspend fun setOnboardingDone(done: Boolean) = set(ONBOARDING_DONE, done)

    fun getStatusBarEnabled(): Flow<Boolean> = get(STATUS_BAR_ENABLED, true)
    suspend fun setStatusBarEnabled(enabled: Boolean) = set(STATUS_BAR_ENABLED, enabled)

    fun getShowMainOnly(): Flow<Boolean> = get(SHOW_MAIN_ONLY, true)
    suspend fun setShowMainOnly(mainOnly: Boolean) = set(SHOW_MAIN_ONLY, mainOnly)

    private fun <T> get(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        dataStore.data.map { it[key] ?: defaultValue }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }
}
