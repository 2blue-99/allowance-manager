package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.Announcement
import com.allowance.manager.core.domain.model.Holidays
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.local.dao.CycleDao
import com.allowance.manager.core.local.dao.TransactionDao
import com.allowance.manager.core.local.entity.CycleEntity
import com.allowance.manager.core.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/** 메모리 사이클 테이블 — start 오름차순 정렬을 DAO처럼 보장 */
class FakeCycleDao : CycleDao {
    val rows = MutableStateFlow<List<CycleEntity>>(emptyList())

    override suspend fun upsert(entity: CycleEntity) {
        rows.value = (rows.value.filterNot { it.start == entity.start } + entity).sortedBy { it.start }
    }

    override suspend fun upsertAll(entities: List<CycleEntity>) = entities.forEach { upsert(it) }

    override fun observeAll(): Flow<List<CycleEntity>> = rows

    override suspend fun getAll(): List<CycleEntity> = rows.value.sortedBy { it.start }

    override suspend fun deleteStartingFrom(start: String) {
        rows.value = rows.value.filter { it.start < start }
    }

    override suspend fun updateEnd(start: String, end: String, updatedAt: Long) {
        rows.value = rows.value.map { if (it.start == start) it.copy(endExclusive = end, updatedAt = updatedAt) else it }
    }

    override suspend fun updateBudget(start: String, budget: Long, updatedAt: Long) {
        rows.value = rows.value.map { if (it.start == start) it.copy(budget = budget, updatedAt = updatedAt) else it }
    }

    override suspend fun pinEnd(start: String, end: String, updatedAt: Long) {
        rows.value = rows.value.map {
            if (it.start == start) it.copy(endExclusive = end, endPinned = true, updatedAt = updatedAt) else it
        }
    }
}

/**
 * 거래는 시각 목록만 흉내낸다 — 관문은 가장 오래된 시각, 사이클 목록은 전체 시각만 본다.
 * 그 외 메서드는 호출되면 실패시켜 의도치 않은 의존을 드러낸다.
 */
class FakeTransactionDao : TransactionDao {
    /** 거래 시각들(epoch ms). 비면 거래 없음 */
    val times = MutableStateFlow<List<Long>>(emptyList())

    /** 관문 백필 기준 — 직접 지정하거나, 없으면 [times]의 최솟값 */
    var firstTime: Long? = null
        get() = field ?: times.value.minOrNull()

    fun add(date: LocalDate) {
        times.value = times.value + date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    override suspend fun getFirstTransactionTime(): Long? = firstTime
    override fun observeAllTimes(): Flow<List<Long>> = times.map { it.sortedDescending() }

    override suspend fun insert(entity: TransactionEntity): Long = unused()
    override suspend fun update(entity: TransactionEntity) = unused()
    override suspend fun delete(entity: TransactionEntity) = unused()
    override suspend fun getById(id: Long): TransactionEntity? = unused()
    override suspend fun getLastTransactionTime(): Long? = unused()
    override fun observeAll(): Flow<List<TransactionEntity>> = unused()
    override fun observeBetween(start: Long, end: Long): Flow<List<TransactionEntity>> = unused()
    override fun observeBudgetSpentBetween(start: Long, end: Long): Flow<Long> = unused()
    override fun observeBudgetIncomeBetween(start: Long, end: Long): Flow<Long> = unused()
    override fun observeLedgerSpentBetween(start: Long, end: Long): Flow<Long> = unused()
    override fun observeLedgerIncomeBetween(start: Long, end: Long): Flow<Long> = unused()
    override suspend fun getUnmatched(): List<TransactionEntity> = unused()
    override suspend fun promoteByIds(ids: List<Long>, accountId: Long) = unused()
    override suspend fun getWithExtractedAccount(): List<TransactionEntity> = unused()
    override suspend fun deleteByIds(ids: List<Long>) = unused()
    override suspend fun countBySource(packageName: String): Int = unused()
    override suspend fun deleteBySource(packageName: String) = unused()
    override suspend fun promoteBySource(packageName: String, accountId: Long) = unused()

    private fun unused(): Nothing = error("테스트가 준비하지 않은 TransactionDao 메서드가 호출됨")
}

/** 공휴일 데이터만 갈아끼우는 원격 설정 (프로퍼티명은 getHolidays()와의 JVM 시그니처 충돌 회피) */
class FakeRemoteConfigRepository : RemoteConfigRepository {
    var holidayData: Holidays = Holidays.EMPTY
    override suspend fun fetchAndActivate(): Boolean = true
    override fun getForcedUpdateVersion(): String = ""
    override fun getRecommendUpdateVersion(): String = ""
    override fun getHolidays(): Holidays = holidayData
    override fun getAnnouncement(): Announcement? = null
}
