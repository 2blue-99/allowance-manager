package com.allowance.manager.feature.stats

import com.allowance.manager.core.analytics.AnalyticsHelper
import com.allowance.manager.core.domain.model.Announcement
import com.allowance.manager.core.domain.model.BudgetAlertSetting
import com.allowance.manager.core.domain.model.BudgetAlertState
import com.allowance.manager.core.domain.model.Cycle
import com.allowance.manager.core.domain.model.DailyReminderSetting
import com.allowance.manager.core.domain.model.Holidays
import com.allowance.manager.core.domain.model.LedgerFilter
import com.allowance.manager.core.domain.model.PaydayAlertSetting
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

// ViewModel 테스트용 페이크 — 화면이 실제로 쓰는 UseCase를 진짜로 조립하고, 저장소만 갈아끼운다.
// (core:domain 테스트의 페이크는 모듈 밖에서 안 보여 필요한 만큼만 다시 둔다)

class FakeCycleRepository(var rows: List<Cycle>) : CycleRepository {
    override fun observeCycleAt(date: LocalDate): Flow<Cycle> = flowOf(pick(date))
    override suspend fun cycleAt(date: LocalDate): Cycle = pick(date)
    override fun observeAll(): Flow<List<Cycle>> = flowOf(rows)
    override suspend fun getAll(): List<Cycle> = rows
    override suspend fun setBudget(cycleStart: LocalDate, amount: Long) = unused()
    override suspend fun budgetFor(cycleStart: LocalDate): Long = rows.firstOrNull { it.start == cycleStart }?.budget ?: 0L
    override suspend fun setCycleEnd(cycleStart: LocalDate, end: LocalDate) = unused()
    override suspend fun moveCycleStart(boundary: LocalDate, today: LocalDate) = unused()
    override suspend fun init(payday: Int, today: LocalDate) = unused()
    override suspend fun changePayday(boundary: LocalDate, payday: Int, today: LocalDate) = unused()

    private fun pick(date: LocalDate): Cycle = rows.lastOrNull { !it.start.isAfter(date) } ?: rows.first()
    private fun unused(): Nothing = error("테스트가 준비하지 않은 CycleRepository 메서드가 호출됨")
}

class FakeTransactionRepository : TransactionRepository {
    var spentBetween: (Long, Long) -> Long = { _, _ -> 0L }
    var allTimes: List<Long> = emptyList()
    var between: (Long, Long) -> List<Transaction> = { _, _ -> emptyList() }

    override fun observeBudgetSpentBetween(start: Long, end: Long): Flow<Long> = flowOf(spentBetween(start, end))
    override fun observeBudgetIncomeBetween(start: Long, end: Long): Flow<Long> = flowOf(0L)
    override fun observeAllTimes(): Flow<List<Long>> = flowOf(allTimes)
    override fun observeBetween(start: Long, end: Long): Flow<List<Transaction>> = flowOf(between(start, end))

    override suspend fun record(transaction: Transaction): Long = unused()
    override suspend fun update(transaction: Transaction) = unused()
    override suspend fun delete(id: Long) = unused()
    override suspend fun getById(id: Long): Transaction? = unused()
    override suspend fun getFirstTransactionTime(): Long? = unused()
    override suspend fun getLastTransactionTime(): Long? = unused()
    override fun observeAll(): Flow<List<Transaction>> = unused()
    override fun observeLedgerSpentBetween(start: Long, end: Long): Flow<Long> = unused()
    override fun observeLedgerIncomeBetween(start: Long, end: Long): Flow<Long> = unused()
    override suspend fun setHidden(id: Long, hidden: Boolean) = unused()
    override suspend fun setScope(id: Long, scope: TxScope) = unused()
    override suspend fun promoteToMain(pattern: String, accountId: Long) = unused()
    override suspend fun promoteToMainBySource(packageName: String, accountId: Long) = unused()
    override suspend fun countMatchingForIgnore(pattern: String?, packageName: String): Int = unused()
    override suspend fun deleteMatchingForIgnore(pattern: String?, packageName: String) = unused()

    private fun unused(): Nothing = error("테스트가 준비하지 않은 TransactionRepository 메서드가 호출됨")
}

class FakeRemoteConfigRepository : RemoteConfigRepository {
    override suspend fun fetchAndActivate(): Boolean = true
    override fun getForcedUpdateVersion(): String = ""
    override fun getRecommendUpdateVersion(): String = ""
    override fun getHolidays(): Holidays = Holidays.EMPTY
    override fun getAnnouncement(): Announcement? = null
}

/** 사용자 유형만 준다 — 통계는 호칭 외엔 DataStore를 안 쓴다 */
class FakeDataStoreRepository : DataStoreRepository {
    override fun getUserType(): Flow<UserType> = flowOf(UserType.Default)
    override suspend fun setUserType(type: UserType) = unused()
    override fun getIntroShown(): Flow<Boolean> = unused()
    override suspend fun setIntroShown(shown: Boolean) = unused()
    override fun getOnboardingDone(): Flow<Boolean> = unused()
    override suspend fun setOnboardingDone(done: Boolean) = unused()
    override fun getStatusBarEnabled(): Flow<Boolean> = unused()
    override suspend fun setStatusBarEnabled(enabled: Boolean) = unused()
    override fun getHomeFilter(): Flow<LedgerFilter> = unused()
    override suspend fun setHomeFilter(filter: LedgerFilter) = unused()
    override fun getCalendarFilter(): Flow<LedgerFilter> = unused()
    override suspend fun setCalendarFilter(filter: LedgerFilter) = unused()
    override fun getHomeGuideShown(): Flow<Boolean> = unused()
    override suspend fun setHomeGuideShown(shown: Boolean) = unused()
    override fun getHomeNewAccountBadge(): Flow<Boolean> = unused()
    override suspend fun setHomeNewAccountBadge(show: Boolean) = unused()
    override fun getBudgetAlertSetting(): Flow<BudgetAlertSetting> = unused()
    override suspend fun setBudgetAlertSetting(setting: BudgetAlertSetting) = unused()
    override fun getDailyReminderSetting(): Flow<DailyReminderSetting> = unused()
    override suspend fun setDailyReminderSetting(setting: DailyReminderSetting) = unused()
    override fun getPaydayAlertSetting(): Flow<PaydayAlertSetting> = unused()
    override suspend fun setPaydayAlertSetting(setting: PaydayAlertSetting) = unused()
    override suspend fun getPaydayAlertLastSent(): String = unused()
    override suspend fun setPaydayAlertLastSent(stamp: String) = unused()
    override suspend fun getBudgetAlertState(): BudgetAlertState = unused()
    override suspend fun setBudgetAlertState(state: BudgetAlertState) = unused()
    override suspend fun getReminderLastSeen(): Long = unused()
    override suspend fun setReminderLastSeen(timeMs: Long) = unused()
    override suspend fun getReminderLastNotified(): Long = unused()
    override suspend fun setReminderLastNotified(timeMs: Long) = unused()
    override suspend fun getLastSeenAnnouncementId(): String = unused()
    override suspend fun setLastSeenAnnouncementId(id: String) = unused()
    override suspend fun getRecommendUpdateLastShown(): String = unused()
    override suspend fun setRecommendUpdateLastShown(date: String) = unused()

    private fun unused(): Nothing = error("테스트가 준비하지 않은 DataStoreRepository 메서드가 호출됨")
}

class FakeAnalyticsHelper : AnalyticsHelper {
    val events = mutableListOf<String>()
    override fun logScreenView(screenName: String) {}
    override fun logEvent(name: String, params: Map<String, Any?>) { events += name }
    override fun setUserProperty(name: String, value: String?) {}
    override fun recordNonFatal(throwable: Throwable) {}
}
