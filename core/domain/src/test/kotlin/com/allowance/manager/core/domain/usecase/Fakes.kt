package com.allowance.manager.core.domain.usecase

import com.allowance.manager.core.domain.model.Announcement
import com.allowance.manager.core.domain.model.Cycle
import com.allowance.manager.core.domain.model.Holidays
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.model.IgnoredAccount
import com.allowance.manager.core.domain.model.MaskedAccount
import com.allowance.manager.core.domain.model.LedgerFilter
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.model.BudgetAlertSetting
import com.allowance.manager.core.domain.model.BudgetAlertState
import com.allowance.manager.core.domain.model.DailyReminderSetting
import com.allowance.manager.core.domain.model.PaydayAlertSetting
import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.IgnoredAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.ZoneId

/** 사이클 행 고정 목록 — 관문 없이 주어진 행만 돌려준다 */
class FakeCycleRepository(var rows: List<Cycle>) : CycleRepository {
    override fun observeCycleAt(date: LocalDate): Flow<Cycle> = flowOf(cycleAtSync(date))
    override suspend fun cycleAt(date: LocalDate): Cycle = cycleAtSync(date)
    override fun observeAll(): Flow<List<Cycle>> = flowOf(rows)
    override suspend fun getAll(): List<Cycle> = rows
    override suspend fun setBudget(cycleStart: LocalDate, amount: Long) {
        rows = rows.map { if (it.start == cycleStart) it.copy(budget = amount) else it }
    }
    override suspend fun budgetFor(cycleStart: LocalDate): Long = rows.firstOrNull { it.start == cycleStart }?.budget ?: 0L
    override suspend fun setCycleEnd(cycleStart: LocalDate, end: LocalDate) = error("unused")
    override suspend fun moveCycleStart(boundary: LocalDate, today: LocalDate) = error("unused")
    override suspend fun init(payday: Int, today: LocalDate) = error("unused")
    override suspend fun changePayday(boundary: LocalDate, payday: Int, today: LocalDate) = error("unused")

    private fun cycleAtSync(date: LocalDate): Cycle =
        rows.lastOrNull { !it.start.isAfter(date) } ?: rows.first()
}

/** 필요한 조회만 람다로 갈아끼우는 거래 저장소 — 그 외 호출은 실패시켜 의도치 않은 의존을 드러낸다 */
open class FakeTransactionRepository : TransactionRepository {
    var spentBetween: (Long, Long) -> Long = { _, _ -> 0L }
    var incomeBetween: (Long, Long) -> Long = { _, _ -> 0L }
    var allTimes: List<Long> = emptyList()
    var between: (Long, Long) -> List<Transaction> = { _, _ -> emptyList() }

    override fun observeBudgetSpentBetween(start: Long, end: Long): Flow<Long> = flowOf(spentBetween(start, end))
    override fun observeBudgetIncomeBetween(start: Long, end: Long): Flow<Long> = flowOf(incomeBetween(start, end))
    override fun observeAllTimes(): Flow<List<Long>> = flowOf(allTimes)
    override fun observeBetween(start: Long, end: Long): Flow<List<Transaction>> = flowOf(between(start, end))

    override suspend fun record(transaction: Transaction): Long = unused()
    override suspend fun update(transaction: Transaction): Unit = unused()
    override suspend fun delete(id: Long): Unit = unused()
    override suspend fun getById(id: Long): Transaction? = unused()
    override suspend fun getFirstTransactionTime(): Long? = unused()
    override suspend fun getLastTransactionTime(): Long? = unused()
    override fun observeAll(): Flow<List<Transaction>> = unused()
    override fun observeLedgerSpentBetween(start: Long, end: Long): Flow<Long> = unused()
    override fun observeLedgerIncomeBetween(start: Long, end: Long): Flow<Long> = unused()
    override suspend fun setHidden(id: Long, hidden: Boolean): Unit = unused()
    override suspend fun setScope(id: Long, scope: TxScope): Unit = unused()
    override suspend fun promoteToMain(pattern: String, accountId: Long): Unit = unused()
    override suspend fun promoteToMainBySource(packageName: String, accountId: Long): Unit = unused()
    override suspend fun demoteFromMain(accountId: Long): Unit = unused()
    override suspend fun updateContent(
        id: Long, type: TransactionType, amount: Long, merchant: String?, memo: String?, category: TransactionCategory?,
    ): Unit = unused()
    override suspend fun countMatchingForIgnore(pattern: String?, packageName: String): Int = unused()
    override suspend fun deleteMatchingForIgnore(pattern: String?, packageName: String): Unit = unused()

    private fun unused(): Nothing = error("테스트가 준비하지 않은 TransactionRepository 메서드가 호출됨")
}

/** 공휴일 데이터만 갈아끼우는 원격 설정 (프로퍼티명은 getHolidays()와의 JVM 시그니처 충돌 회피) */
class FakeRemoteConfigRepository(var holidayData: Holidays = Holidays.EMPTY) : RemoteConfigRepository {
    override suspend fun fetchAndActivate(): Boolean = true
    override fun getForcedUpdateVersion(): String = ""
    override fun getRecommendUpdateVersion(): String = ""
    override fun getHolidays(): Holidays = holidayData
    override fun getAnnouncement(): Announcement? = null
}

/** 2026년 날짜 축약 */
fun d(month: Int, day: Int): LocalDate = LocalDate.of(2026, month, day)

fun LocalDate.millis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** 사이클 행 축약 — 예산 기본 50만 · 규칙 25일 */
fun cycle(start: LocalDate, end: LocalDate, budget: Long = 500_000L, payday: Int = 25) =
    Cycle(start = start, endExclusive = end, budget = budget, payday = payday)

/**
 * 인메모리 거래 저장소 — 기록·조회·내용 수정·무시 소급을 실제 저장소처럼 흉내낸다.
 * (RecordTransaction · UpdateTransaction · AddManualTransaction · 무시 UseCase 테스트용)
 */
class InMemoryTransactionRepository : FakeTransactionRepository() {
    val rows = mutableListOf<Transaction>()
    private var nextId = 1L

    override suspend fun record(transaction: Transaction): Long {
        val saved = transaction.copy(id = nextId++); rows += saved; return saved.id
    }
    override suspend fun getById(id: Long): Transaction? = rows.firstOrNull { it.id == id }
    override suspend fun updateContent(
        id: Long, type: TransactionType, amount: Long, merchant: String?, memo: String?, category: TransactionCategory?,
    ) {
        val i = rows.indexOfFirst { it.id == id }; if (i < 0) return
        rows[i] = rows[i].copy(type = type, amount = amount, merchant = merchant, memo = memo, category = category)
    }
    override suspend fun setScope(id: Long, scope: TxScope) {
        val i = rows.indexOfFirst { it.id == id }; if (i >= 0) rows[i] = rows[i].copy(scope = scope)
    }
    override suspend fun delete(id: Long) { rows.removeAll { it.id == id } }

    // 무시 소급: 번호 기준은 마스킹 대조, 출처 기준은 같은 앱의 계좌번호 없는 내역 (실제 구현과 동일 규칙)
    private fun matchesIgnore(t: Transaction, pattern: String?, packageName: String): Boolean =
        if (pattern != null) MaskedAccount.matches(t.extractedAccount, pattern)
        else t.packageName == packageName && t.extractedAccount.isNullOrBlank()
    override suspend fun countMatchingForIgnore(pattern: String?, packageName: String): Int =
        rows.count { matchesIgnore(it, pattern, packageName) }
    override suspend fun deleteMatchingForIgnore(pattern: String?, packageName: String) {
        rows.removeAll { matchesIgnore(it, pattern, packageName) }
    }
}

/** 인메모리 계좌 저장소 */
class FakeAccountRepository : AccountRepository {
    val all = MutableStateFlow<List<Account>>(emptyList())
    private var nextId = 1L

    fun seed(account: Account): Account = account.copy(id = nextId++).also { all.value = all.value + it }
    fun byId(id: Long): Account? = all.value.firstOrNull { it.id == id }

    override fun observeAll(): Flow<List<Account>> = all
    override suspend fun getEnabled(): List<Account> = all.value.filter { it.enabled }
    override suspend fun getAll(): List<Account> = all.value
    override suspend fun getById(id: Long): Account? = byId(id)
    override suspend fun add(account: Account): Long = seed(account).id
    override suspend fun update(account: Account) { all.value = all.value.map { if (it.id == account.id) account else it } }
    override suspend fun delete(account: Account) { all.value = all.value.filterNot { it.id == account.id } }
}

/** 인메모리 무시 계좌 저장소 */
class FakeIgnoredAccountRepository : IgnoredAccountRepository {
    val rows = mutableListOf<IgnoredAccount>()
    private var nextId = 1L
    override fun observeAll(): Flow<List<IgnoredAccount>> = flowOf(rows.toList())
    override suspend fun getAll(): List<IgnoredAccount> = rows.toList()
    override suspend fun add(account: IgnoredAccount): Long { val saved = account.copy(id = nextId++); rows += saved; return saved.id }
    override suspend fun delete(id: Long) { rows.removeAll { it.id == id } }
}

/** DataStore 스텁 — 새 계좌 배지만 기록하고, 그 외 호출은 실패시켜 의도치 않은 의존을 드러낸다 */
open class FakeDataStoreRepository : DataStoreRepository {
    var newAccountBadge: Boolean = false

    override fun getUserType(): Flow<UserType> = unused()
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
    override fun getHomeNewAccountBadge(): Flow<Boolean> = flowOf(newAccountBadge)
    override suspend fun setHomeNewAccountBadge(show: Boolean) { newAccountBadge = show }
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
