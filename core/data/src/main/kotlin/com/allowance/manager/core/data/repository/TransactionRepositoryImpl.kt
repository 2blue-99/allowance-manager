package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.MaskedAccount
import com.allowance.manager.core.domain.model.MonthlyExpense
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.PaydayRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import com.allowance.manager.core.local.dao.TransactionDao
import com.allowance.manager.core.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val paydayRepository: PaydayRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
) : TransactionRepository {

    override suspend fun record(transaction: Transaction): Long =
        transactionDao.insert(transaction.toEntity(cycleStartOf(transaction.createdAt)))

    override suspend fun update(transaction: Transaction) =
        transactionDao.update(transaction.toEntity(cycleStartOf(transaction.createdAt)))

    /**
     * 그 시각의 거래가 속한 사이클 시작일.
     *
     * 사이클 경계는 SQL이 계산할 수 없어 저장 시점에 박아둔다. 월급일 이력이 소스이고,
     * 아직 이력이 없으면 DataStore 규칙일로 계산한다([BudgetCycle.of]와 같은 규칙).
     */
    private suspend fun cycleStartOf(createdAt: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val date = java.time.Instant.ofEpochMilli(createdAt).atZone(zone).toLocalDate()
        val cycle = BudgetCycle.of(
            rules = paydayRepository.history(),
            today = date,
            holidays = remoteConfigRepository.getHolidays(),
            fallbackPayday = dataStoreRepository.getPayday().first(),
        )
        return cycle.start.toString()
    }

    override suspend fun delete(id: Long) {
        transactionDao.getById(id)?.let { transactionDao.delete(it) }
    }

    override suspend fun getById(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun getFirstTransactionTime(): Long? =
        transactionDao.getFirstTransactionTime()

    override suspend fun getLastTransactionTime(): Long? =
        transactionDao.getLastTransactionTime()

    override fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeBetween(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.observeBetween(start, end).map { list -> list.map { it.toDomain() } }

    override fun observeBudgetSpentBetween(start: Long, end: Long): Flow<Long> =
        transactionDao.observeBudgetSpentBetween(start, end)

    override fun observeBudgetIncomeBetween(start: Long, end: Long): Flow<Long> =
        transactionDao.observeBudgetIncomeBetween(start, end)

    // ── 사이클 기준 ──

    override fun observeBudgetSpentInCycle(cycleStart: LocalDate): Flow<Long> =
        transactionDao.observeBudgetSpentInCycle(cycleStart.toString())

    override fun observeBudgetIncomeInCycle(cycleStart: LocalDate): Flow<Long> =
        transactionDao.observeBudgetIncomeInCycle(cycleStart.toString())

    override fun observeByCycle(cycleStart: LocalDate): Flow<List<Transaction>> =
        transactionDao.observeByCycle(cycleStart.toString()).map { list -> list.map { it.toDomain() } }

    override fun observeCycleExpenseTotals(): Flow<Map<LocalDate, Long>> =
        transactionDao.observeCycleExpenseTotals().map { rows ->
            rows.mapNotNull { row ->
                runCatching { LocalDate.parse(row.cycle) }.getOrNull()?.let { it to row.total }
            }.toMap()
        }

    override fun observeTransactionCycles(): Flow<List<LocalDate>> =
        transactionDao.observeTransactionCycles().map { list ->
            list.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        }

    override suspend fun reassignCycle(cycleStart: LocalDate, from: Long, to: Long) =
        transactionDao.reassignCycle(cycleStart.toString(), from, to)

    override fun observeLedgerSpentBetween(start: Long, end: Long): Flow<Long> =
        transactionDao.observeLedgerSpentBetween(start, end)

    override fun observeLedgerIncomeBetween(start: Long, end: Long): Flow<Long> =
        transactionDao.observeLedgerIncomeBetween(start, end)

    override fun observeMonthlyExpenseTotals(): Flow<List<MonthlyExpense>> =
        transactionDao.observeMonthlyExpenseTotals().map { rows ->
            rows.mapNotNull { row ->
                runCatching { YearMonth.parse(row.ym) }.getOrNull()
                    ?.let { MonthlyExpense(yearMonth = it, total = row.total) }
            }
        }

    override fun observeTransactionMonths(): Flow<List<YearMonth>> =
        transactionDao.observeTransactionMonths().map { rows ->
            rows.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }
        }

    override suspend fun setHidden(id: Long, hidden: Boolean) {
        // 전환기: hidden 토글을 scope 로 매핑 (true=EXCLUDED / false=BUDGET). Phase 5에서 setScope 로 대체.
        val scope = if (hidden) "EXCLUDED" else "BUDGET"
        transactionDao.getById(id)?.let { transactionDao.update(it.copy(scope = scope)) }
    }

    override suspend fun setScope(id: Long, scope: TxScope) {
        transactionDao.getById(id)?.let { transactionDao.update(it.copy(scope = scope.name)) }
    }

    // 마스킹 계좌 대조는 SQL로 불가 → 미매칭 내역을 불러와 도메인 규칙으로 판정 후 일괄 승격
    override suspend fun promoteToMain(pattern: String, accountId: Long) {
        val ids = transactionDao.getUnmatched()
            .filter { MaskedAccount.matches(it.extractedAccount, pattern) }
            .map { it.id }
        if (ids.isNotEmpty()) transactionDao.promoteByIds(ids, accountId)
    }

    // 출처(앱) 기준 승격은 SQL로 처리 (계좌번호 없는 같은 packageName 내역)
    override suspend fun promoteToMainBySource(packageName: String, accountId: Long) =
        transactionDao.promoteBySource(packageName, accountId)

    // 번호 기준은 마스킹 대조가 필요해 도메인에서 필터, 출처 기준은 SQL로 처리
    override suspend fun countMatchingForIgnore(pattern: String?, packageName: String): Int =
        if (pattern != null) {
            transactionDao.getWithExtractedAccount()
                .count { MaskedAccount.matches(it.extractedAccount, pattern) }
        } else {
            transactionDao.countBySource(packageName)
        }

    override suspend fun deleteMatchingForIgnore(pattern: String?, packageName: String) {
        if (pattern != null) {
            val ids = transactionDao.getWithExtractedAccount()
                .filter { MaskedAccount.matches(it.extractedAccount, pattern) }
                .map { it.id }
            if (ids.isNotEmpty()) transactionDao.deleteByIds(ids)
        } else {
            transactionDao.deleteBySource(packageName)
        }
    }
}

private fun Transaction.toEntity(cycleStart: String) = TransactionEntity(
    id = id,
    type = type.name,
    amount = amount,
    balance = balance,
    packageName = packageName,
    sourceName = sourceName,
    merchant = merchant,
    extractedAccount = extractedAccount,
    accountId = accountId,
    category = category?.name,
    memo = memo,
    scope = scope.name,
    isManual = isManual,
    createdAt = createdAt,
    cycleStart = cycleStart,
)

private fun TransactionEntity.toDomain() = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    amount = amount,
    balance = balance,
    packageName = packageName,
    sourceName = sourceName,
    merchant = merchant,
    extractedAccount = extractedAccount,
    accountId = accountId,
    category = category?.let { runCatching { TransactionCategory.valueOf(it) }.getOrNull() },
    memo = memo,
    scope = runCatching { TxScope.valueOf(scope) }.getOrDefault(TxScope.BUDGET),
    isManual = isManual,
    createdAt = createdAt,
)
