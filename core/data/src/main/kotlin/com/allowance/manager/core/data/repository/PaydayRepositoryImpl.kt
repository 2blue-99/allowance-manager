package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.PaydayRule
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.PaydayRepository
import com.allowance.manager.core.local.dao.PaydayHistoryDao
import com.allowance.manager.core.local.entity.PaydayHistoryEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaydayRepositoryImpl @Inject constructor(
    private val paydayHistoryDao: PaydayHistoryDao,
    private val dataStoreRepository: DataStoreRepository,
) : PaydayRepository {

    // LocalDate.toString() == "yyyy-MM-dd" (ISO). 문자열 정렬이 곧 시간 정렬.
    override suspend fun ruleAt(date: LocalDate): PaydayRule =
        paydayHistoryDao.ruleAt(date.toString())?.toDomain() ?: fallbackRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRuleAt(date: LocalDate): Flow<PaydayRule> =
        paydayHistoryDao.observeRuleAt(date.toString())
            .flatMapLatest { entity ->
                val rule = entity?.toDomain()
                if (rule != null) flowOf(rule)
                else dataStoreRepository.getPayday().map { PaydayRule(PaydayRule.FLOOR, it) }
            }

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

    override suspend fun removeRule(effectiveDate: LocalDate) =
        paydayHistoryDao.delete(effectiveDate.toString())

    /**
     * 이력이 아직 없을 때 쓰는 규칙 — DataStore의 규칙일이 [PaydayRule.FLOOR]부터 유효했다고 본다.
     *
     * DB에 쓰지 않고 읽을 때만 대신하므로, 사용자가 고르지 않은 값이 이력에 남는 일이 없다.
     * 정상 경로에서는 온보딩이 첫 줄을 만들기 때문에 이 값이 쓰일 일은 거의 없다.
     */
    private suspend fun fallbackRule(): PaydayRule =
        PaydayRule(PaydayRule.FLOOR, dataStoreRepository.getPayday().first())
}

/** 날짜 파싱이 깨진 행은 버린다(수동 편집·손상 대비). */
private fun PaydayHistoryEntity.toDomain(): PaydayRule? =
    runCatching { LocalDate.parse(effectiveDate) }.getOrNull()
        ?.let { PaydayRule(effectiveDate = it, payday = payday) }
