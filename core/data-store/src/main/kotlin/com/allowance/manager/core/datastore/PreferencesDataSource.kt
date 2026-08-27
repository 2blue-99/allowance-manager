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
import kotlinx.coroutines.flow.first
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
        // 달별 월급일 지정("이번 달 월급일 조정"). "2026-08:21,2026-09:23" — 최근 12개월만 보존
        val PAYDAY_OVERRIDES = stringPreferencesKey("payday_overrides")
        val INTRO_SHOWN = booleanPreferencesKey("intro_shown")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val STATUS_BAR_ENABLED = booleanPreferencesKey("status_bar_enabled")
        val HOME_SHOW_MAIN = booleanPreferencesKey("home_show_main")      // 홈 필터 - 메인 포함
        val HOME_SHOW_HIDDEN = booleanPreferencesKey("home_show_hidden")  // 홈 필터 - 숨김 포함
        val CAL_SHOW_MAIN = booleanPreferencesKey("cal_show_main")        // 월별 필터 - 메인 포함
        val CAL_SHOW_HIDDEN = booleanPreferencesKey("cal_show_hidden")    // 월별 필터 - 숨김 포함
        val USER_TYPE = stringPreferencesKey("user_type")           // 사용자 유형 (student/youth/common)
        val HOME_GUIDE_SHOWN = booleanPreferencesKey("home_guide_shown")  // 홈 최초 진입 가이드 노출 여부
        val HOME_NEW_ACCOUNT_BADGE = booleanPreferencesKey("home_new_account_badge")  // 새(미등록) 계좌 감지 → 전체 칩 빨간 점
        // 예산 소진 알림 — 켜짐 여부 + 빈도(low/medium/high)
        val BUDGET_ALERT_ENABLED = booleanPreferencesKey("budget_alert_enabled")
        val BUDGET_ALERT_FREQUENCY = stringPreferencesKey("budget_alert_frequency")
        // 가계부 관리 알림(정산 리마인더) — 켜짐 여부 + 시각(하루 분 단위, 0~1439)
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_MINUTES = intPreferencesKey("daily_reminder_minutes")
        // 월급일 알림 — 켜짐 여부 + 마지막 발송 표식("2026-08-24:TODAY", 같은 날 중복 발송 방지용)
        val PAYDAY_ALERT_ENABLED = booleanPreferencesKey("payday_alert_enabled")
        val PAYDAY_ALERT_LAST_SENT = stringPreferencesKey("payday_alert_last_sent")
        // 예산 소진 알림 발송 상태(내부) — 사이클 시작 millis + 발송한 임계값 CSV
        val BUDGET_ALERT_CYCLE = longPreferencesKey("budget_alert_cycle")
        val BUDGET_ALERT_FIRED = stringPreferencesKey("budget_alert_fired")
        // 가계부 관리 알림(내부) — 마지막 앱 조회 시각 / 마지막 리마인더 발송 시각
        val REMINDER_LAST_SEEN = longPreferencesKey("reminder_last_seen")
        val REMINDER_LAST_NOTIFIED = longPreferencesKey("reminder_last_notified")
        // 홈 공지 — 마지막으로 확인(닫음)한 공지 id (같은 공지 재노출 방지)
        val LAST_SEEN_ANNOUNCEMENT_ID = stringPreferencesKey("last_seen_announcement_id")
        // 추천 업데이트 팝업 — 마지막으로 띄운 날짜(yyyy-MM-dd). 하루 1회 노출 판정용.
        val RECOMMEND_UPDATE_LAST_SHOWN = stringPreferencesKey("recommend_update_last_shown")

        private const val DEFAULT_PAYDAY = 25
        private const val DEFAULT_USER_TYPE = "common"              // UserType.Default.key
        private const val DEFAULT_ALERT_FREQUENCY = "medium"        // AlertFrequency.Default.key
        private const val DEFAULT_REMINDER_MINUTES = 21 * 60        // 오후 9:00
    }

    fun getPayday(): Flow<Int> = get(PAYDAY, DEFAULT_PAYDAY)
    suspend fun setPayday(day: Int) = set(PAYDAY, day)

    // 달별 월급일 지정 — 파싱·직렬화는 도메인(PaydayOverrides)이 담당하고 여기선 문자열만 다룬다.
    fun getPaydayOverrides(): Flow<String> = get(PAYDAY_OVERRIDES, "")
    suspend fun setPaydayOverrides(raw: String) = set(PAYDAY_OVERRIDES, raw)

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

    fun getHomeNewAccountBadge(): Flow<Boolean> = get(HOME_NEW_ACCOUNT_BADGE, false)
    suspend fun setHomeNewAccountBadge(show: Boolean) = set(HOME_NEW_ACCOUNT_BADGE, show)

    // 예산 소진 알림 — enabled + frequency를 한 플로우로 읽고 한 트랜잭션으로 쓴다. Pair = (켜짐, 빈도키)
    fun getBudgetAlert(): Flow<Pair<Boolean, String>> = dataStore.data
        .map { (it[BUDGET_ALERT_ENABLED] ?: true) to (it[BUDGET_ALERT_FREQUENCY] ?: DEFAULT_ALERT_FREQUENCY) }
        .distinctUntilChanged()
    suspend fun setBudgetAlert(enabled: Boolean, frequencyKey: String) {
        dataStore.edit {
            it[BUDGET_ALERT_ENABLED] = enabled
            it[BUDGET_ALERT_FREQUENCY] = frequencyKey
        }
    }

    // 가계부 관리 알림 — enabled + 시각(분)을 한 플로우로 읽고 한 트랜잭션으로 쓴다. Pair = (켜짐, 분)
    fun getDailyReminder(): Flow<Pair<Boolean, Int>> = dataStore.data
        .map { (it[DAILY_REMINDER_ENABLED] ?: true) to (it[DAILY_REMINDER_MINUTES] ?: DEFAULT_REMINDER_MINUTES) }
        .distinctUntilChanged()
    suspend fun setDailyReminder(enabled: Boolean, minutesOfDay: Int) {
        dataStore.edit {
            it[DAILY_REMINDER_ENABLED] = enabled
            it[DAILY_REMINDER_MINUTES] = minutesOfDay
        }
    }

    // 월급일 알림 — 켜짐 여부는 Flow, 마지막 발송 표식은 1회성 읽기/쓰기
    fun getPaydayAlertEnabled(): Flow<Boolean> = get(PAYDAY_ALERT_ENABLED, true)
    suspend fun setPaydayAlertEnabled(enabled: Boolean) = set(PAYDAY_ALERT_ENABLED, enabled)
    suspend fun getPaydayAlertLastSent(): String = dataStore.data.first()[PAYDAY_ALERT_LAST_SENT] ?: ""
    suspend fun setPaydayAlertLastSent(stamp: String) = set(PAYDAY_ALERT_LAST_SENT, stamp)

    // 예산 소진 알림 발송 상태 — 한 번만 읽고 쓰는 내부 상태(Flow 아님). Pair = (사이클 millis, 발송 CSV)
    suspend fun getBudgetAlertState(): Pair<Long, String> {
        val prefs = dataStore.data.first()
        return (prefs[BUDGET_ALERT_CYCLE] ?: 0L) to (prefs[BUDGET_ALERT_FIRED] ?: "")
    }
    suspend fun setBudgetAlertState(cycleStart: Long, firedCsv: String) {
        dataStore.edit {
            it[BUDGET_ALERT_CYCLE] = cycleStart
            it[BUDGET_ALERT_FIRED] = firedCsv
        }
    }

    // 가계부 관리 알림 타임스탬프 (내부 상태, 1회성 읽기/쓰기)
    suspend fun getReminderLastSeen(): Long = dataStore.data.first()[REMINDER_LAST_SEEN] ?: 0L
    suspend fun setReminderLastSeen(timeMs: Long) = set(REMINDER_LAST_SEEN, timeMs)
    suspend fun getReminderLastNotified(): Long = dataStore.data.first()[REMINDER_LAST_NOTIFIED] ?: 0L
    suspend fun setReminderLastNotified(timeMs: Long) = set(REMINDER_LAST_NOTIFIED, timeMs)

    // 홈 공지 — 마지막으로 확인한 공지 id (1회성 읽기/쓰기)
    suspend fun getLastSeenAnnouncementId(): String = dataStore.data.first()[LAST_SEEN_ANNOUNCEMENT_ID] ?: ""
    suspend fun setLastSeenAnnouncementId(id: String) = set(LAST_SEEN_ANNOUNCEMENT_ID, id)

    // 추천 업데이트 팝업 — 마지막으로 띄운 날짜 (1회성 읽기/쓰기)
    suspend fun getRecommendUpdateLastShown(): String = dataStore.data.first()[RECOMMEND_UPDATE_LAST_SHOWN] ?: ""
    suspend fun setRecommendUpdateLastShown(date: String) = set(RECOMMEND_UPDATE_LAST_SHOWN, date)

    private fun <T> get(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        dataStore.data.map { it[key] ?: defaultValue }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }
}
