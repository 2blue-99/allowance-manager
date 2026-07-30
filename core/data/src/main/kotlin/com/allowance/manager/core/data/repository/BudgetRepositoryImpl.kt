package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.MonthlyBudget
import com.allowance.manager.core.domain.repository.BudgetRepository
import com.allowance.manager.core.local.dao.BudgetDao
import com.allowance.manager.core.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
) : BudgetRepository {

    // YearMonth.toString() == "yyyy-MM" (ISO). 문자열 정렬이 곧 시간 정렬.
    override suspend fun setBudgetForMonth(month: YearMonth, amount: Long) =
        budgetDao.upsert(
            BudgetEntity(
                effectiveMonth = month.toString(),
                amount = amount,
                updatedAt = System.currentTimeMillis(),
            ),
        )

    override suspend fun getBudgetForMonth(month: YearMonth): Long =
        budgetDao.getBudgetForMonth(month.toString()) ?: 0L

    override fun observeBudgetForMonth(month: YearMonth): Flow<Long> =
        budgetDao.observeBudgetForMonth(month.toString()).map { it ?: 0L }

    override fun observeHistory(): Flow<List<MonthlyBudget>> =
        budgetDao.observeAll().map { list ->
            list.mapNotNull { e ->
                runCatching { YearMonth.parse(e.effectiveMonth) }.getOrNull()
                    ?.let { MonthlyBudget(effectiveMonth = it, amount = e.amount) }
            }
        }
}
