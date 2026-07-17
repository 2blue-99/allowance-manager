package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.repository.TransactionRepository
import com.allowance.manager.core.local.dao.TransactionDao
import com.allowance.manager.core.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeBetween(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.observeBetween(start, end).map { list -> list.map { it.toDomain() } }

    override fun observeSpentBetween(start: Long, end: Long): Flow<Long> =
        transactionDao.observeSpentBetween(start, end)

    override suspend fun setIgnored(id: Long, ignored: Boolean) {
        transactionDao.getById(id)?.let { transactionDao.update(it.copy(isIgnored = ignored)) }
    }

    override suspend fun promoteToMain(pattern: String, accountId: Long) =
        transactionDao.promoteByPattern(pattern, accountId)
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
    memo = memo,
    isIgnored = isIgnored,
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
    memo = memo,
    isIgnored = isIgnored,
    createdAt = createdAt,
)
