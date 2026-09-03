package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.MonthlyBudget
import com.allowance.manager.core.domain.repository.BudgetRepository
import com.allowance.manager.core.local.dao.BudgetDao
import com.allowance.manager.core.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
) : BudgetRepository {

    // LocalDate.toString() == "yyyy-MM-dd" (ISO). 문자열 정렬이 곧 시간 정렬.
    override suspend fun setBudgetForCycle(cycleStart: LocalDate, amount: Long) =
        budgetDao.upsert(
            BudgetEntity(
                effectiveCycle = cycleStart.toString(),
                amount = amount,
                updatedAt = System.currentTimeMillis(),
            ),
        )

    override suspend fun getBudgetForCycle(cycleStart: LocalDate): Long =
        budgetDao.getBudgetForCycle(cycleStart.toString()) ?: 0L

    override fun observeBudgetForCycle(cycleStart: LocalDate): Flow<Long> =
        budgetDao.observeBudgetForCycle(cycleStart.toString()).map { it ?: 0L }

    override fun observeHistory(): Flow<List<MonthlyBudget>> =
        budgetDao.observeAll().map { list ->
            list.mapNotNull { e ->
                runCatching { LocalDate.parse(e.effectiveCycle) }.getOrNull()
                    ?.let { MonthlyBudget(effectiveCycle = it, amount = e.amount) }
            }
        }
}
