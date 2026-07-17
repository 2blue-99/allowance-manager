package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.Spending
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.repository.SpendingRepository
import com.allowance.manager.core.local.dao.SpendingDao
import com.allowance.manager.core.local.entity.SpendingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingRepositoryImpl @Inject constructor(
    private val spendingDao: SpendingDao,
) : SpendingRepository {

    override suspend fun insertSpending(spending: Spending) {
        spendingDao.insert(spending.toEntity())
    }

    override fun getAllSpendings(): Flow<List<Spending>> =
        spendingDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
}

private fun Spending.toEntity() = SpendingEntity(
    id = id,
    type = type.name,
    amount = amount,
    totalAmount = totalAmount,
    bankName = bankName,
    timestamp = timestamp,
    memo = memo,
)

private fun SpendingEntity.toDomain() = Spending(
    id = id,
    type = TransactionType.valueOf(type),
    amount = amount,
    totalAmount = totalAmount,
    bankName = bankName,
    timestamp = timestamp,
    memo = memo,
)
