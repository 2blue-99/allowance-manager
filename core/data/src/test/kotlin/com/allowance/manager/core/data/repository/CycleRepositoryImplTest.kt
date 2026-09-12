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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * 사이클 저장소 관문 검증 — 자동 생성·끝 재계산·백필·경계 변경.
 *
 * 2026년 달력 기준 (공휴일 없음): 7/25(토)→7/24, 8/25(화), 9/25(금), 9/30(수), 10/25(일)→10/23, 10/10(토)→10/9.
 * 오늘은 2026-09-12로 고정한다.
 */
class CycleRepositoryImplTest {

    private val today = LocalDate.of(2026, 9, 12)
    private val cycleDao = FakeCycleDao()
    private val transactionDao = FakeTransactionDao()
    private val remoteConfig = FakeRemoteConfigRepository()

    private fun repo(today: LocalDate = this.today) =
        CycleRepositoryImpl(cycleDao, transactionDao, remoteConfig) { today }

    private fun d(month: Int, day: Int): LocalDate = LocalDate.of(2026, month, day)

    private fun seed(start: LocalDate, end: LocalDate, payday: Int, budget: Long = 500_000L) {
        cycleDao.rows.value = cycleDao.rows.value + CycleEntity(
            start = start.toString(),
            endExclusive = end.toString(),
            budget = budget,
            payday = payday,
            updatedAt = 0L,
        )
    }

    private fun starts() = cycleDao.rows.value.map { LocalDate.parse(it.start) }

    // ─────────────────────────── 행이 없을 때 ───────────────────────────

    @Test
    fun `행이 없으면 기본 규칙(25일)으로 계산만 하고 저장하지 않는다`() = runBlocking {
        val cycle = repo().cycleAt(today)

        assertEquals(d(8, 25), cycle.start)
        assertEquals(d(9, 25), cycle.endExclusive)
        assertEquals(0L, cycle.budget)
        assertTrue(cycleDao.rows.value.isEmpty())
    }

    @Test
    fun `init은 오늘이 속한 사이클을 첫 행으로 심는다`() = runBlocking {
        repo().init(payday = 25, today = today)

        val row = cycleDao.rows.value.single()
        assertEquals("2026-08-25", row.start)
        assertEquals("2026-09-25", row.endExclusive)
        assertEquals(25, row.payday)
        assertEquals(0L, row.budget)
    }

    // ─────────────────────────── 관문: 앞으로 채우기 ───────────────────────────

    @Test
    fun `마지막 행이 오늘을 못 덮으면 규칙일로 이어 생성하고 예산을 이월한다`() = runBlocking {
        seed(d(6, 25), d(7, 25), payday = 25, budget = 500_000L)

        val cycle = repo().cycleAt(today)

        // 6/25~7/24(토 보정) → 7/24~8/25 → 8/25~9/25
        assertEquals(listOf(d(6, 25), d(7, 24), d(8, 25)), starts())
        assertEquals(d(8, 25), cycle.start)
        assertEquals(d(9, 25), cycle.endExclusive)
        assertTrue(cycleDao.rows.value.all { it.budget == 500_000L })
    }

    @Test
    fun `마지막 행의 끝(예정)은 공휴일이 바뀌면 다시 계산된다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25)
        remoteConfig.holidayData = Holidays(byDate = mapOf(d(9, 25) to "임시공휴일"))

        val cycle = repo().cycleAt(today)

        // 9/25(금)이 공휴일 → 9/24(목)
        assertEquals(d(9, 24), cycle.endExclusive)
        assertEquals("2026-09-24", cycleDao.rows.value.single().endExclusive)
    }

    @Test
    fun `지나간 행은 건드리지 않는다`() = runBlocking {
        seed(d(7, 24), d(8, 25), payday = 25, budget = 400_000L)
        seed(d(8, 25), d(9, 25), payday = 25, budget = 500_000L)

        repo().cycleAt(today)

        val first = cycleDao.rows.value.first()
        assertEquals("2026-08-25", first.endExclusive)
        assertEquals(400_000L, first.budget)
    }

    // ─────────────────────────── 관문: 백필 ───────────────────────────

    @Test
    fun `첫 행보다 오래된 거래가 있으면 뒤로도 채워 모든 거래가 사이클에 속하게 한다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25, budget = 500_000L)
        transactionDao.firstTime = d(7, 10).millis()

        repo().cycleAt(today)

        // 7/10을 덮을 때까지: 7/24~8/25, 6/25~7/24
        assertEquals(listOf(d(6, 25), d(7, 24), d(8, 25)), starts())
        assertTrue(cycleDao.rows.value.all { it.budget == 500_000L })
    }

    @Test
    fun `거래가 첫 행 안에 있으면 백필하지 않는다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25)
        transactionDao.firstTime = d(9, 1).millis()

        repo().cycleAt(today)

        assertEquals(listOf(d(8, 25)), starts())
    }

    // ─────────────────────────── 조회 ───────────────────────────

    @Test
    fun `첫 행보다 앞 날짜를 물으면 규칙으로 역산한 가상 사이클을 준다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25)

        val cycle = repo().cycleAt(d(7, 1))

        assertEquals(d(6, 25), cycle.start)
        assertEquals(d(7, 24), cycle.endExclusive)
        assertEquals(0L, cycle.budget)
        assertEquals(listOf(d(8, 25)), starts())   // 저장은 안 함
    }

    @Test
    fun `observeCycleAt은 행 변경을 따라 다시 방출한다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25, budget = 100_000L)
        val repo = repo()

        assertEquals(100_000L, repo.observeCycleAt(today).first().budget)
        repo.setBudget(d(8, 25), 300_000L)
        assertEquals(300_000L, repo.observeCycleAt(today).first().budget)
    }

    @Test
    fun `setBudget은 그 행만 바꾸고 다음에 생성되는 행이 그 값을 이월한다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25, budget = 0L)
        val repo = repo(today = d(10, 1))

        repo.setBudget(d(8, 25), 300_000L)
        val next = repo.cycleAt(d(10, 1))

        // 10/25(일) → 10/23(금)
        assertEquals(d(9, 25), next.start)
        assertEquals(d(10, 23), next.endExclusive)
        assertEquals(300_000L, next.budget)
    }

    // ─────────────────────────── 월급일 변경 ───────────────────────────

    @Test
    fun `changePayday - 경계 유지 규칙 변경은 시작을 두고 끝만 재계산한다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25, budget = 500_000L)

        repo().changePayday(boundary = d(8, 25), payday = 0, today = today)

        val row = cycleDao.rows.value.single()
        assertEquals("2026-08-25", row.start)
        assertEquals("2026-09-30", row.endExclusive)   // 말일
        assertEquals(0, row.payday)
        assertEquals(500_000L, row.budget)              // 예산 이어받음
    }

    @Test
    fun `changePayday - 경계를 사이클 중간으로 찍으면 현재 행을 끊고 새 행을 시작한다`() = runBlocking {
        seed(d(7, 24), d(8, 25), payday = 25, budget = 400_000L)
        seed(d(8, 25), d(9, 25), payday = 25, budget = 500_000L)

        // 이직: 9/5에 받았고 앞으로 15일
        repo().changePayday(boundary = d(9, 5), payday = 15, today = today)

        val rows = cycleDao.rows.value
        assertEquals(listOf(d(7, 24), d(8, 25), d(9, 5)), starts())
        assertEquals("2026-09-05", rows[1].endExclusive)    // 현재 행이 경계에서 끊김
        assertEquals("2026-09-15", rows[2].endExclusive)    // 새 규칙으로 계산
        assertEquals(15, rows[2].payday)
        assertEquals(500_000L, rows[2].budget)
    }

    @Test
    fun `changePayday - 경계를 이전 사이클로 당기면 현재 행은 지우고 이전 행 끝을 맞춘다`() = runBlocking {
        seed(d(7, 24), d(8, 25), payday = 25, budget = 400_000L)
        seed(d(8, 25), d(9, 25), payday = 25, budget = 500_000L)

        // "사실 8/22에 받았어" — 규칙은 그대로
        repo().changePayday(boundary = d(8, 22), payday = 25, today = today)

        val rows = cycleDao.rows.value
        assertEquals(listOf(d(7, 24), d(8, 22)), starts())
        assertEquals("2026-08-22", rows[0].endExclusive)
        assertEquals(400_000L, rows[0].budget)              // 이전 행 예산 보존
        assertEquals("2026-09-25", rows[1].endExclusive)    // 8/25는 3일 뒤라 건너뛰고 9/25
        assertEquals(500_000L, rows[1].budget)              // 현재 행 예산 이어받음
    }

    @Test
    fun `changePayday - 경계를 멀리 당겨도 오늘까지 이어 채운다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25)

        repo().changePayday(boundary = d(7, 20), payday = 10, today = today)

        // 7/20~8/10 → 8/10~9/10 → 9/10~10/9(토 보정)
        assertEquals(listOf(d(7, 20), d(8, 10), d(9, 10)), starts())
        val last = cycleDao.rows.value.last()
        assertEquals("2026-10-09", last.endExclusive)
        assertTrue(!today.isBefore(LocalDate.parse(last.start)) && today.isBefore(LocalDate.parse(last.endExclusive)))
    }

    @Test
    fun `changePayday - 행이 없으면 경계에서 첫 행을 만든다`() = runBlocking {
        repo().changePayday(boundary = d(9, 5), payday = 15, today = today)

        val row = cycleDao.rows.value.single()
        assertEquals("2026-09-05", row.start)
        assertEquals("2026-09-15", row.endExclusive)
        assertEquals(0L, row.budget)
    }

    // ─────────────────────────── 이번 회차만 받을 날 (끝 고정) ───────────────────────────

    @Test
    fun `setCycleEnd - 고정한 끝은 관문이 규칙으로 되돌리지 않는다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25)
        val repo = repo()

        repo.setCycleEnd(d(8, 25), d(9, 20))
        val cycle = repo.cycleAt(today)

        assertEquals(d(9, 20), cycle.endExclusive)
        assertTrue(cycleDao.rows.value.single().endPinned)
    }

    @Test
    fun `setCycleEnd - 고정한 끝이 지나면 다음 회차가 거기서 시작하고 이후 끝은 다시 규칙으로`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25, budget = 500_000L)
        val repo = repo(today = d(9, 21))

        repo.setCycleEnd(d(8, 25), d(9, 20))
        val cycle = repo.cycleAt(d(9, 21))

        // 8/25~9/20(고정) → 9/20~10/23(10/25 일요일 보정, 규칙 복귀)
        assertEquals(listOf(d(8, 25), d(9, 20)), starts())
        assertEquals(d(9, 20), cycle.start)
        assertEquals(d(10, 23), cycle.endExclusive)
        assertTrue(!cycle.endPinned)
        assertEquals(500_000L, cycle.budget)
    }

    @Test
    fun `setCycleEnd - 규칙일을 바꾸면 고정이 풀려 끝이 다시 계산된다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25)
        val repo = repo()
        repo.setCycleEnd(d(8, 25), d(9, 20))

        repo.changePayday(boundary = d(8, 25), payday = 25, today = today)

        val row = cycleDao.rows.value.single()
        assertEquals("2026-09-25", row.endExclusive)
        assertTrue(!row.endPinned)
    }

    @Test
    fun `budgetFor - 없는 사이클은 0`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25, budget = 500_000L)

        assertEquals(500_000L, repo().budgetFor(d(8, 25)))
        assertEquals(0L, repo().budgetFor(d(1, 1)))
        assertNull(cycleDao.rows.value.firstOrNull { it.start == "2026-01-01" })
    }

    private fun LocalDate.millis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

// ─────────────────────────── 페이크 ───────────────────────────

/** 메모리 사이클 테이블 — start 오름차순 정렬을 DAO처럼 보장 */
private class FakeCycleDao : CycleDao {
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

/** 관문은 가장 오래된 거래 시각만 본다 — 그 외는 호출되면 실패시켜 의도치 않은 의존을 드러낸다 */
private class FakeTransactionDao : TransactionDao {
    var firstTime: Long? = null

    override suspend fun getFirstTransactionTime(): Long? = firstTime

    override suspend fun insert(entity: TransactionEntity): Long = unused()
    override suspend fun update(entity: TransactionEntity) = unused()
    override suspend fun delete(entity: TransactionEntity) = unused()
    override suspend fun getById(id: Long): TransactionEntity? = unused()
    override suspend fun getLastTransactionTime(): Long? = unused()
    override fun observeAll(): Flow<List<TransactionEntity>> = unused()
    override fun observeBetween(start: Long, end: Long): Flow<List<TransactionEntity>> = unused()
    override fun observeBudgetSpentBetween(start: Long, end: Long): Flow<Long> = unused()
    override fun observeBudgetIncomeBetween(start: Long, end: Long): Flow<Long> = unused()
    override fun observeAllTimes(): Flow<List<Long>> = unused()
    override fun observeLedgerSpentBetween(start: Long, end: Long): Flow<Long> = unused()
    override fun observeLedgerIncomeBetween(start: Long, end: Long): Flow<Long> = unused()
    override suspend fun getUnmatched(): List<TransactionEntity> = unused()
    override suspend fun promoteByIds(ids: List<Long>, accountId: Long) = unused()
    override suspend fun getWithExtractedAccount(): List<TransactionEntity> = unused()
    override suspend fun deleteByIds(ids: List<Long>) = unused()
    override suspend fun countBySource(packageName: String): Int = unused()
    override suspend fun deleteBySource(packageName: String) = unused()
    override suspend fun promoteBySource(packageName: String, accountId: Long) = unused()

    private fun unused(): Nothing = error("CycleRepository가 쓰지 않는 DAO 메서드가 호출됨")
}

/** 공휴일 데이터만 갈아끼우는 원격 설정 (프로퍼티명은 getHolidays()와의 JVM 시그니처 충돌 회피) */
private class FakeRemoteConfigRepository : RemoteConfigRepository {
    var holidayData: Holidays = Holidays.EMPTY
    override suspend fun fetchAndActivate(): Boolean = true
    override fun getForcedUpdateVersion(): String = ""
    override fun getRecommendUpdateVersion(): String = ""
    override fun getHolidays(): Holidays = holidayData
    override fun getAnnouncement(): Announcement? = null
}
