package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.PaydayRule
import com.allowance.manager.core.domain.repository.PaydayRepository
import com.allowance.manager.core.local.dao.PaydayHistoryDao
import com.allowance.manager.core.local.entity.PaydayHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaydayRepositoryImpl @Inject constructor(
    private val paydayHistoryDao: PaydayHistoryDao,
) : PaydayRepository {

    // LocalDate.toString() == "yyyy-MM-dd" (ISO). 문자열 정렬이 곧 시간 정렬.
    override suspend fun ruleAt(date: LocalDate): PaydayRule? =
        paydayHistoryDao.ruleAt(date.toString())?.toDomain()

    override fun observeRuleAt(date: LocalDate): Flow<PaydayRule?> =
        paydayHistoryDao.observeRuleAt(date.toString()).map { it?.toDomain() }

    override fun observeHistory(): Flow<List<PaydayRule>> =
        paydayHistoryDao.observeAll().map { list -> list.mapNotNull { it.toDomain() } }

    override suspend fun history(): List<PaydayRule> =
        paydayHistoryDao.getAll().mapNotNull { it.toDomain() }

    override suspend fun setRule(effectiveDate: LocalDate, payday: Int) {
        paydayHistoryDao.upsert(
            PaydayHistoryEntity(
                effectiveDate = effectiveDate.toString(),
                payday = payday.coerceIn(0, 31),
                updatedAt = System.currentTimeMillis(),
            ),
        )
        paydayHistoryDao.trimTo(PaydayRule.MAX_ENTRIES)
    }

    override suspend fun removeRulesAfter(date: LocalDate) =
        paydayHistoryDao.deleteAfter(date.toString())

    override suspend fun removeRule(effectiveDate: LocalDate) =
        paydayHistoryDao.delete(effectiveDate.toString())
}

/** 날짜 파싱이 깨진 행은 버린다(수동 편집·손상 대비). */
private fun PaydayHistoryEntity.toDomain(): PaydayRule? =
    runCatching { LocalDate.parse(effectiveDate) }.getOrNull()
        ?.let { PaydayRule(effectiveDate = it, payday = payday) }
