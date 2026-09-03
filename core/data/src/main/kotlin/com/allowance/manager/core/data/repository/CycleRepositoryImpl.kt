package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.Cycle
import com.allowance.manager.core.domain.model.Holidays
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.local.dao.CycleDao
import com.allowance.manager.core.local.dao.TransactionDao
import com.allowance.manager.core.local.entity.CycleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사이클 저장소 구현.
 *
 * 모든 읽기 경로(홈·위젯·상태바·알림 리스너)가 [ensure] 관문을 지난다.
 * 위젯 갱신과 리스너가 동시에 들어와 "새 행 추가"가 겹칠 수 있어 [mutex]로 잠근다.
 */
@Singleton
class CycleRepositoryImpl @Inject constructor(
    private val cycleDao: CycleDao,
    private val transactionDao: TransactionDao,
    private val remoteConfigRepository: RemoteConfigRepository,
) : CycleRepository {

    private val mutex = Mutex()

    /** 온보딩 전(행 없음) 폴백 규칙일 — 계산에만 쓰고 저장하지 않는다 */
    private val fallbackPayday = 25

    override fun observeCycleAt(date: LocalDate): Flow<Cycle> = flow {
        runCatching { ensure(date) }
        emitAll(cycleDao.observeAll().map { rows -> pick(rows.toDomain(), date) })
    }

    override suspend fun cycleAt(date: LocalDate): Cycle {
        runCatching { ensure(date) }
        return pick(cycleDao.getAll().toDomain(), date)
    }

    override fun observeAll(): Flow<List<Cycle>> = flow {
        runCatching { ensure(LocalDate.now()) }
        emitAll(cycleDao.observeAll().map { it.toDomain() })
    }

    override suspend fun getAll(): List<Cycle> {
        runCatching { ensure(LocalDate.now()) }
        return cycleDao.getAll().toDomain()
    }

    override suspend fun setBudget(cycleStart: LocalDate, amount: Long) =
        cycleDao.updateBudget(cycleStart.toString(), amount, System.currentTimeMillis())

    override suspend fun budgetFor(cycleStart: LocalDate): Long =
        cycleDao.getAll().toDomain().firstOrNull { it.start == cycleStart }?.budget ?: 0L

    override suspend fun init(payday: Int, today: LocalDate) {
        mutex.withLock {
            val cycle = BudgetCycle.of(payday.coerceIn(0, 31), today, holidays())
            // 재실행(프로세스 중단 후 온보딩 재진입) 대비: 이 시작일 이후 행을 걷어내고 다시 심는다
            cycleDao.deleteStartingFrom(cycle.start.toString())
            cycleDao.upsert(
                CycleEntity(
                    start = cycle.start.toString(),
                    endExclusive = cycle.endExclusive.toString(),
                    budget = 0L,
                    payday = payday.coerceIn(0, 31),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun changePayday(boundary: LocalDate, payday: Int, today: LocalDate) {
        mutex.withLock {
            val rows = cycleDao.getAll().toDomain()
            val rule = payday.coerceIn(0, 31)
            // 대체되는 예산은 현재 사이클 값을 이어받는다 (행이 없으면 0)
            val keepBudget = rows.lastOrNull { !it.start.isAfter(today) }?.budget
                ?: rows.lastOrNull()?.budget ?: 0L

            // [boundary] 이후 시작 행은 이번 변경으로 대체 — 삭제
            cycleDao.deleteStartingFrom(boundary.toString())

            // 직전 행의 끝을 새 경계로 (트림/연장) — 인접성 유지
            val previous = cycleDao.getAll().toDomain().lastOrNull { it.start.isBefore(boundary) }
            if (previous != null && previous.endExclusive != boundary) {
                cycleDao.updateEnd(previous.start.toString(), boundary.toString(), System.currentTimeMillis())
            }

            cycleDao.upsert(
                CycleEntity(
                    start = boundary.toString(),
                    endExclusive = deriveEnd(boundary, rule).toString(),
                    budget = keepBudget,
                    payday = rule,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            // 경계를 과거로 당겼으면 오늘까지 이어서 채운다
            ensureLocked(today)
        }
    }

    // ─────────────────────────── 관문 ───────────────────────────

    /** [date](와 오늘)까지 사이클 행이 이어져 있도록 보장. 행이 하나도 없으면(온보딩 전) 아무것도 안 한다. */
    private suspend fun ensure(date: LocalDate) {
        mutex.withLock { ensureLocked(maxOf(date, LocalDate.now())) }
    }

    private suspend fun ensureLocked(cover: LocalDate) {
        val rows = cycleDao.getAll().toDomain()
        if (rows.isEmpty()) return

        val holidays = holidays()
        val now = System.currentTimeMillis()

        // ① 마지막 행의 끝은 '예정' — 규칙·공휴일이 바뀌었을 수 있으니 재계산해 맞춘다
        var last = rows.last()
        val derived = deriveEnd(last.start, last.payday, holidays)
        if (derived != last.endExclusive) {
            cycleDao.updateEnd(last.start.toString(), derived.toString(), now)
            last = last.copy(endExclusive = derived)
        }

        // ② 앞으로 채우기 — 마지막 행이 [cover]를 덮을 때까지 규칙일로 이어 생성 (예산 이월)
        var probes = 0
        while (!cover.isBefore(last.endExclusive) && probes++ < MAX_PROBE) {
            val next = Cycle(
                start = last.endExclusive,
                endExclusive = deriveEnd(last.endExclusive, last.payday, holidays),
                budget = last.budget,
                payday = last.payday,
            )
            cycleDao.upsert(next.toEntity(now))
            last = next
        }

        // ③ 백필 — 가장 오래된 거래가 첫 행보다 과거면 뒤로도 생성 (모든 거래가 사이클에 속하게)
        val firstTxDate = transactionDao.getFirstTransactionTime()?.toLocalDate() ?: return
        var first = rows.first()
        probes = 0
        while (firstTxDate.isBefore(first.start) && probes++ < MAX_PROBE) {
            val start = BudgetCycle.previousPayDateBefore(first.start, first.payday, holidays)
            if (!start.isBefore(first.start)) break   // 더 못 거슬러 오르면 중단
            val previous = Cycle(
                start = start,
                endExclusive = first.start,
                budget = first.budget,
                payday = first.payday,
            )
            cycleDao.upsert(previous.toEntity(now))
            first = previous
        }
    }

    // ─────────────────────────── 계산 ───────────────────────────

    /** [date]를 덮는 행. 행 밖(첫 행 이전)이면 규칙으로 역산한 가상 사이클, 행이 없으면 기본 규칙 계산. */
    private suspend fun pick(rows: List<Cycle>, date: LocalDate): Cycle {
        if (rows.isEmpty()) {
            // 온보딩 전 — 기본 규칙일로 계산만 (저장하지 않음)
            val cycle = BudgetCycle.of(fallbackPayday, date, holidays())
            return Cycle(cycle.start, cycle.endExclusive, budget = 0L, payday = fallbackPayday)
        }
        rows.lastOrNull { !it.start.isAfter(date) }?.let { row ->
            // 관문이 오늘까지 채우므로 보통 여기서 끝난다. (미래 조회면 마지막 행을 준다)
            return if (date.isBefore(row.endExclusive)) row else rows.last()
        }
        // 첫 행보다 과거 — 백필이 거래 범위는 덮으므로 드묾. 첫 행 규칙으로 역산한 가상 사이클.
        val first = rows.first()
        val virtual = BudgetCycle.of(
            rules = rows.map { com.allowance.manager.core.domain.model.PaydayRule(it.start, it.payday) },
            today = date,
            holidays = holidays(),
            fallbackPayday = first.payday,
        )
        return Cycle(virtual.start, virtual.endExclusive, budget = 0L, payday = first.payday)
    }

    /** 다음 지급일(주말·공휴일 보정) — 최소 하루짜리 사이클 보장 */
    private suspend fun deriveEnd(start: LocalDate, payday: Int): LocalDate =
        deriveEnd(start, payday, holidays())

    private fun deriveEnd(start: LocalDate, payday: Int, holidays: Holidays): LocalDate {
        val candidate = BudgetCycle.nextPayDateAfter(start, payday, holidays)
        return if (candidate.isAfter(start)) candidate else start.plusDays(1)
    }

    private fun holidays(): Holidays = remoteConfigRepository.getHolidays()

    private fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    companion object {
        /** 채우기·백필 한도 — 무한 루프 방지 (월 주기 기준 50년) */
        private const val MAX_PROBE = 600
    }
}

/** 날짜 파싱이 깨진 행은 버린다(수동 편집·손상 대비). */
private fun List<CycleEntity>.toDomain(): List<Cycle> = mapNotNull { e ->
    runCatching {
        Cycle(
            start = LocalDate.parse(e.start),
            endExclusive = LocalDate.parse(e.endExclusive),
            budget = e.budget,
            payday = e.payday,
        )
    }.getOrNull()
}

private fun Cycle.toEntity(updatedAt: Long) = CycleEntity(
    start = start.toString(),
    endExclusive = endExclusive.toString(),
    budget = budget,
    payday = payday,
    updatedAt = updatedAt,
)
