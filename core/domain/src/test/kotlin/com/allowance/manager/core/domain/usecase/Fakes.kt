package com.allowance.manager.core.domain.usecase

import com.allowance.manager.core.domain.model.Announcement
import com.allowance.manager.core.domain.model.Cycle
import com.allowance.manager.core.domain.model.Holidays
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
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
