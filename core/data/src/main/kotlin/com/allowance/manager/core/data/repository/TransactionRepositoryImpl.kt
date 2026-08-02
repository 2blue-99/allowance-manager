package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.MaskedAccount
import com.allowance.manager.core.domain.model.MonthlyExpense
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.repository.TransactionRepository
import com.allowance.manager.core.local.dao.TransactionDao
import com.allowance.manager.core.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
) : TransactionRepository {

    override suspend fun record(transaction: Transaction): Long =
        transactionDao.insert(transaction.toEntity())

    override suspend fun update(transaction: Transaction) =
        transactionDao.update(transaction.toEntity())

    override suspend fun delete(id: Long) {
        transactionDao.getById(id)?.let { transactionDao.delete(it) }
    }

    override suspend fun getById(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun getFirstTransactionTime(): Long? =
        transactionDao.getFirstTransactionTime()

    override fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeBetween(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.observeBetween(start, end).map { list -> list.map { it.toDomain() } }

    override fun observeSpentBetween(start: Long, end: Long): Flow<Long> =
        transactionDao.observeSpentBetween(start, end)

    override fun observeMonthlyExpenseTotals(): Flow<List<MonthlyExpense>> =
        transactionDao.observeMonthlyExpenseTotals().map { rows ->
            rows.mapNotNull { row ->
                runCatching { YearMonth.parse(row.ym) }.getOrNull()
                    ?.let { MonthlyExpense(yearMonth = it, total = row.total) }
            }
        }

    override suspend fun setIgnored(id: Long, ignored: Boolean) {
        transactionDao.getById(id)?.let { transactionDao.update(it.copy(isIgnored = ignored)) }
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
}

private fun Transaction.toEntity() = TransactionEntity(
    id = id,
    type = type.name,
    amount = amount,
    balance = balance,
    packageName = packageName,
    sourceName = sourceName,
    extractedAccount = extractedAccount,
    accountId = accountId,
    category = category?.name,
    memo = memo,
    isIgnored = isIgnored,
    isManual = isManual,
    createdAt = createdAt,
)

private fun TransactionEntity.toDomain() = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    amount = amount,
    balance = balance,
    packageName = packageName,
    sourceName = sourceName,
    extractedAccount = extractedAccount,
    accountId = accountId,
    category = category?.let { runCatching { TransactionCategory.valueOf(it) }.getOrNull() },
    memo = memo,
    isIgnored = isIgnored,
    isManual = isManual,
    createdAt = createdAt,
)
