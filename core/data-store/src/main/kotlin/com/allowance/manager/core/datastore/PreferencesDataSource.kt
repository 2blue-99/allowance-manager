package com.allowance.manager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
        // 월 예산(용돈)은 budget_history 테이블로 이관 (월별 이력)
        val PAYDAY = intPreferencesKey("payday")                    // 수급일. 1~31, 0 = 말일
        val INTRO_SHOWN = booleanPreferencesKey("intro_shown")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val STATUS_BAR_ENABLED = booleanPreferencesKey("status_bar_enabled")
        val SHOW_MAIN_ONLY = booleanPreferencesKey("show_main_only")
        val USER_TYPE = stringPreferencesKey("user_type")           // 사용자 유형 (student/youth/common)

        private const val DEFAULT_PAYDAY = 25
        private const val DEFAULT_USER_TYPE = "common"              // UserType.Default.key
    }

    fun getPayday(): Flow<Int> = get(PAYDAY, DEFAULT_PAYDAY)
    suspend fun setPayday(day: Int) = set(PAYDAY, day)

    fun getUserType(): Flow<String> = get(USER_TYPE, DEFAULT_USER_TYPE)
    suspend fun setUserType(key: String) = set(USER_TYPE, key)

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
