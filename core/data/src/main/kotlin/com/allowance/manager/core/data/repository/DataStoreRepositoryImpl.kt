package com.allowance.manager.core.data.repository

import com.allowance.manager.core.datastore.PreferencesDataSource
import com.allowance.manager.core.domain.model.AlertFrequency
import com.allowance.manager.core.domain.model.BudgetAlertSetting
import com.allowance.manager.core.domain.model.BudgetAlertState
import com.allowance.manager.core.domain.model.DailyReminderSetting
import com.allowance.manager.core.domain.model.LedgerFilter
import com.allowance.manager.core.domain.model.PaydayAlertSetting
import com.allowance.manager.core.domain.model.PaydayOverrides
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
) : DataStoreRepository {

    override fun getPayday(): Flow<Int> = preferencesDataSource.getPayday()
    override suspend fun setPayday(day: Int) = preferencesDataSource.setPayday(day)

    override fun getPaydayOverrides(): Flow<Map<YearMonth, Int>> =
        preferencesDataSource.getPaydayOverrides().map { PaydayOverrides.parse(it) }

    // 읽고-바꿔-쓰기. 사용자가 다이얼로그에서 저장할 때만 호출되는 단발 액션이라 동시 쓰기 경합은 없다.
    override suspend fun setPaydayOverride(month: YearMonth, day: Int?) {
        val current = PaydayOverrides.parse(preferencesDataSource.getPaydayOverrides().first())
        preferencesDataSource.setPaydayOverrides(PaydayOverrides.format(PaydayOverrides.put(current, month, day)))
    }

    override fun getUserType(): Flow<UserType> =
        preferencesDataSource.getUserType().map { UserType.fromKey(it) }
    override suspend fun setUserType(type: UserType) =
        preferencesDataSource.setUserType(type.key)

    override fun getIntroShown(): Flow<Boolean> = preferencesDataSource.getIntroShown()
    override suspend fun setIntroShown(shown: Boolean) = preferencesDataSource.setIntroShown(shown)

    override fun getOnboardingDone(): Flow<Boolean> = preferencesDataSource.getOnboardingDone()
    override suspend fun setOnboardingDone(done: Boolean) = preferencesDataSource.setOnboardingDone(done)

    override fun getStatusBarEnabled(): Flow<Boolean> = preferencesDataSource.getStatusBarEnabled()
    override suspend fun setStatusBarEnabled(enabled: Boolean) = preferencesDataSource.setStatusBarEnabled(enabled)

    override fun getHomeFilter(): Flow<LedgerFilter> =
        preferencesDataSource.getHomeFilter().map { (main, hidden) -> LedgerFilter(main, hidden) }
    override suspend fun setHomeFilter(filter: LedgerFilter) =
        preferencesDataSource.setHomeFilter(filter.showMain, filter.showHidden)

    override fun getCalendarFilter(): Flow<LedgerFilter> =
        preferencesDataSource.getCalFilter().map { (main, hidden) -> LedgerFilter(main, hidden) }
    override suspend fun setCalendarFilter(filter: LedgerFilter) =
        preferencesDataSource.setCalFilter(filter.showMain, filter.showHidden)

    override fun getHomeGuideShown(): Flow<Boolean> = preferencesDataSource.getHomeGuideShown()
    override suspend fun setHomeGuideShown(shown: Boolean) = preferencesDataSource.setHomeGuideShown(shown)

    override fun getHomeNewAccountBadge(): Flow<Boolean> = preferencesDataSource.getHomeNewAccountBadge()
    override suspend fun setHomeNewAccountBadge(show: Boolean) = preferencesDataSource.setHomeNewAccountBadge(show)

    override fun getBudgetAlertSetting(): Flow<BudgetAlertSetting> =
        preferencesDataSource.getBudgetAlert().map { (enabled, freqKey) ->
            BudgetAlertSetting(enabled = enabled, frequency = AlertFrequency.fromKey(freqKey))
        }
    override suspend fun setBudgetAlertSetting(setting: BudgetAlertSetting) =
        preferencesDataSource.setBudgetAlert(setting.enabled, setting.frequency.key)

    override fun getDailyReminderSetting(): Flow<DailyReminderSetting> =
        preferencesDataSource.getDailyReminder().map { (enabled, minutes) ->
            DailyReminderSetting.fromMinutes(enabled, minutes)
        }
    override suspend fun setDailyReminderSetting(setting: DailyReminderSetting) =
        preferencesDataSource.setDailyReminder(setting.enabled, setting.minutesOfDay)

    override fun getPaydayAlertSetting(): Flow<PaydayAlertSetting> =
        preferencesDataSource.getPaydayAlertEnabled().map { PaydayAlertSetting(enabled = it) }
    override suspend fun setPaydayAlertSetting(setting: PaydayAlertSetting) =
        preferencesDataSource.setPaydayAlertEnabled(setting.enabled)

    override suspend fun getPaydayAlertLastSent(): String = preferencesDataSource.getPaydayAlertLastSent()
    override suspend fun setPaydayAlertLastSent(stamp: String) =
        preferencesDataSource.setPaydayAlertLastSent(stamp)

    override suspend fun getBudgetAlertState(): BudgetAlertState {
        val (cycleStart, firedCsv) = preferencesDataSource.getBudgetAlertState()
        val fired = firedCsv.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
        return BudgetAlertState(cycleStart = cycleStart, fired = fired)
    }
    override suspend fun setBudgetAlertState(state: BudgetAlertState) =
        preferencesDataSource.setBudgetAlertState(state.cycleStart, state.fired.sorted().joinToString(","))

    override suspend fun getReminderLastSeen(): Long = preferencesDataSource.getReminderLastSeen()
    override suspend fun setReminderLastSeen(timeMs: Long) = preferencesDataSource.setReminderLastSeen(timeMs)

    override suspend fun getReminderLastNotified(): Long = preferencesDataSource.getReminderLastNotified()
    override suspend fun setReminderLastNotified(timeMs: Long) = preferencesDataSource.setReminderLastNotified(timeMs)

    override suspend fun getLastSeenAnnouncementId(): String = preferencesDataSource.getLastSeenAnnouncementId()
    override suspend fun setLastSeenAnnouncementId(id: String) =
        preferencesDataSource.setLastSeenAnnouncementId(id)

    override suspend fun getRecommendUpdateLastShown(): String = preferencesDataSource.getRecommendUpdateLastShown()
    override suspend fun setRecommendUpdateLastShown(date: String) =
        preferencesDataSource.setRecommendUpdateLastShown(date)
}
