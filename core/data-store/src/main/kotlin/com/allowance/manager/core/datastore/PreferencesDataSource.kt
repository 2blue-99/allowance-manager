package com.allowance.manager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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
        val HOME_SHOW_MAIN = booleanPreferencesKey("home_show_main")      // 홈 필터 - 메인 포함
        val HOME_SHOW_HIDDEN = booleanPreferencesKey("home_show_hidden")  // 홈 필터 - 숨김 포함
        val CAL_SHOW_MAIN = booleanPreferencesKey("cal_show_main")        // 월별 필터 - 메인 포함
        val CAL_SHOW_HIDDEN = booleanPreferencesKey("cal_show_hidden")    // 월별 필터 - 숨김 포함
        val USER_TYPE = stringPreferencesKey("user_type")           // 사용자 유형 (student/youth/common)
        val HOME_GUIDE_SHOWN = booleanPreferencesKey("home_guide_shown")  // 홈 최초 진입 가이드 노출 여부

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

    // 필터 두 키를 한 플로우로 읽고(중간 튐 방지) 한 트랜잭션으로 쓴다. Pair = (메인, 숨김)
    // 홈 기본 = 전체(false,false) — 이후 사용자가 고른 값은 저장됨
    fun getHomeFilter(): Flow<Pair<Boolean, Boolean>> = dataStore.data
        .map { (it[HOME_SHOW_MAIN] ?: false) to (it[HOME_SHOW_HIDDEN] ?: false) }
        .distinctUntilChanged()
    suspend fun setHomeFilter(main: Boolean, hidden: Boolean) {
        dataStore.edit {
            it[HOME_SHOW_MAIN] = main
            it[HOME_SHOW_HIDDEN] = hidden
        }
    }

    // 월별 기본 = 전체(false,false)
    fun getCalFilter(): Flow<Pair<Boolean, Boolean>> = dataStore.data
        .map { (it[CAL_SHOW_MAIN] ?: false) to (it[CAL_SHOW_HIDDEN] ?: false) }
        .distinctUntilChanged()
    suspend fun setCalFilter(main: Boolean, hidden: Boolean) {
        dataStore.edit {
            it[CAL_SHOW_MAIN] = main
            it[CAL_SHOW_HIDDEN] = hidden
        }
    }

    fun getHomeGuideShown(): Flow<Boolean> = get(HOME_GUIDE_SHOWN, false)
    suspend fun setHomeGuideShown(shown: Boolean) = set(HOME_GUIDE_SHOWN, shown)

    private fun <T> get(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        dataStore.data.map { it[key] ?: defaultValue }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }
}
