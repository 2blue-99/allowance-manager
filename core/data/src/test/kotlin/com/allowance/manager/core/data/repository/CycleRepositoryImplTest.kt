package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.Announcement
import com.allowance.manager.core.domain.model.BudgetCycle
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
        // 백필 행 예산은 0 — 예산을 정하기 전 회차라 이월할 근거가 없다 (첫 행만 500,000 유지)
        assertEquals(listOf(0L, 0L, 500_000L), cycleDao.rows.value.map { it.budget })
    }

    @Test
    fun `setBudget - 행이 없는 과거 회차면 그 날까지 백필한 뒤 저장한다 (조용히 무시되지 않음)`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25, budget = 500_000L)
        val repo = repo()
        // 7/24~8/25 회차 — 거래가 없어 행이 없는 상태 (디버그 '예산 넣기' 경로). 8/1은 7/24 지급 뒤라 이 회차
        val julyStart = repo.cycleAt(d(8, 1)).start
        assertEquals(d(7, 24), julyStart)
        assertEquals(listOf(d(8, 25)), starts())

        repo.setBudget(julyStart, 300_000L)

        assertEquals(listOf(d(7, 24), d(8, 25)), starts())
        assertEquals(300_000L, repo.budgetFor(d(7, 24)))
        assertEquals(500_000L, repo.budgetFor(d(8, 25)))   // 기존 행은 그대로
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

    // ─────────────────────────── 받았던 날 (시작만 이동, 끝 유지) ───────────────────────────

    @Test
    fun `moveCycleStart - 말일 규칙에 20을 넣어도 끝은 9월 30일 그대로 (11일짜리 회차가 생기지 않는다)`() = runBlocking {
        seed(d(7, 31), d(8, 31), payday = 0, budget = 400_000L)
        seed(d(8, 31), d(9, 30), payday = 0, budget = 500_000L)

        repo().moveCycleStart(d(8, 20), today)

        val rows = cycleDao.rows.value
        assertEquals(listOf(d(7, 31), d(8, 20)), starts())
        assertEquals("2026-08-20", rows[0].endExclusive)         // 지난 회차 끝 = 새 경계
        assertEquals("2026-09-30", rows[1].endExclusive)         // 이번 회차 끝 유지 (8/31 아님)
        assertTrue(rows[1].endPinned)                            // 시작으로 유도되지 않는 끝 → 고정
        assertEquals(500_000L, rows[1].budget)
        assertEquals(0, rows[1].payday)
    }

    @Test
    fun `moveCycleStart - 경계를 회차 중간으로 찍으면 현재 행이 끊기고 끝은 유지`() = runBlocking {
        seed(d(7, 24), d(8, 25), payday = 25)
        seed(d(8, 25), d(9, 25), payday = 25)

        repo().moveCycleStart(d(9, 5), today)

        val rows = cycleDao.rows.value
        assertEquals(listOf(d(7, 24), d(8, 25), d(9, 5)), starts())
        assertEquals("2026-09-05", rows[1].endExclusive)
        assertEquals("2026-09-25", rows[2].endExclusive)
        assertTrue(rows[2].endPinned)
    }

    @Test
    fun `moveCycleStart - 경계가 현재 시작과 같으면 아무것도 바꾸지 않는다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25)
        val before = cycleDao.rows.value

        repo().moveCycleStart(d(8, 25), today)

        assertEquals(before, cycleDao.rows.value)
    }

    @Test
    fun `moveCycleStart - 옮긴 회차가 지나가면 다음 회차는 규칙으로 계산되고 고정이 아니다`() = runBlocking {
        seed(d(8, 31), d(9, 30), payday = 0)
        val repo = repo(today = d(10, 1))

        repo.moveCycleStart(d(8, 20), d(9, 12))
        val next = repo.cycleAt(d(10, 1))

        // 10/31(토) → 10/30(금)
        assertEquals(d(9, 30), next.start)
        assertEquals(d(10, 30), next.endExclusive)
        assertTrue(!next.endPinned)
    }

    @Test
    fun `moveCycleStart - 이후 규칙을 바꾸면 끝이 새 규칙으로 재계산되고 고정이 풀린다`() = runBlocking {
        seed(d(8, 31), d(9, 30), payday = 0)
        val repo = repo()
        repo.moveCycleStart(d(8, 20), today)

        repo.changePayday(boundary = d(8, 20), payday = 25, today = today)

        val row = cycleDao.rows.value.single()
        assertEquals("2026-09-25", row.endExclusive)   // 8/25는 5일 뒤라 건너뜀
        assertTrue(!row.endPinned)
    }

    @Test
    fun `moveCycleStart - 받을 날로 고정한 끝은 시작을 옮겨도 유지된다`() = runBlocking {
        seed(d(8, 25), d(9, 25), payday = 25)
        val repo = repo()
        repo.setCycleEnd(d(8, 25), d(9, 20))

        repo.moveCycleStart(d(9, 5), today)

        val cycle = repo.cycleAt(today)
        assertEquals(d(9, 5), cycle.start)
        assertEquals(d(9, 20), cycle.endExclusive)
        assertTrue(cycle.endPinned)
    }

    @Test
    fun `moveCycleStart - 행이 없으면 경계에서 첫 행을 만든다`() = runBlocking {
        repo().moveCycleStart(d(9, 5), today)

        val row = cycleDao.rows.value.single()
        assertEquals("2026-09-05", row.start)
        assertEquals("2026-09-25", row.endExclusive)   // 기본 규칙 25일
    }

    // ─────────────────────────── 매트릭스 — 모든 규칙일 × 모든 경계 ───────────────────────────
    // 같은 부류의 오류(특정 규칙·특정 경계 조합에서만 깨지는 것)가 재발하지 않도록 조합을 통째로 훑는다.

    private val paydays = listOf(0, 1, 5, 10, 15, 20, 25, 28, 31)

    /** 규칙 [payday]로 온보딩한 뒤 직전 회차까지 백필된 저장소 */
    private suspend fun freshRepo(payday: Int, today: LocalDate = this.today): Triple<CycleRepositoryImpl, FakeCycleDao, FakeRemoteConfigRepository> {
        val dao = FakeCycleDao()
        val tx = FakeTransactionDao()
        val rc = FakeRemoteConfigRepository()
        val repo = CycleRepositoryImpl(dao, tx, rc) { today }
        repo.init(payday, today)
        // 첫 회차 시작보다 40일 전 거래 → 관문 백필로 직전 회차 행이 생긴다
        tx.firstTime = repo.cycleAt(today).start.minusDays(40).millis()
        repo.cycleAt(today)
        return Triple(repo, dao, rc)
    }

    /** 행들이 빈틈·겹침 없이 이어지고 마지막 행이 오늘을 덮는지 */
    private fun assertContiguous(dao: FakeCycleDao, today: LocalDate) {
        val rows = dao.rows.value
        rows.zipWithNext().forEach { (a, b) -> assertEquals("끊김: ${a.start}~${a.endExclusive} / ${b.start}", a.endExclusive, b.start) }
        val last = rows.last()
        assertTrue("마지막 행이 오늘을 못 덮음: ${last.start}~${last.endExclusive}", today.toString() >= last.start && today.toString() < last.endExclusive)
    }

    @Test
    fun `매트릭스 - moveCycleStart는 모든 규칙·경계에서 끝을 유지하고 예고와 일치한다`() = runBlocking {
        var cases = 0
        for (payday in paydays) {
            val (probe, _, _) = freshRepo(payday)
            val current = probe.cycleAt(today)
            val previousStart = probe.getAll().first { it.endExclusive == current.start }.start
            // 직전 회차 시작 다음날 ~ 오늘까지 모든 경계 (현재 시작 제외)
            var boundary = previousStart.plusDays(1)
            while (!boundary.isAfter(today)) {
                if (boundary != current.start) {
                    val (repo, dao, rc) = freshRepo(payday)
                    val preview = com.allowance.manager.core.domain.usecase.budget.PreviewCycleStartUseCase(repo, rc)(boundary, today)

                    repo.moveCycleStart(boundary, today)

                    val after = repo.cycleAt(today)
                    val label = "규칙=$payday 경계=$boundary"
                    assertEquals(label, boundary, after.start)
                    assertEquals("$label 끝 유지", current.endExclusive, after.endExclusive)
                    assertEquals("$label 예산 유지", current.budget, after.budget)
                    assertEquals("$label 규칙 유지", payday.coerceIn(0, 31), after.payday)
                    assertTrue("$label 고정", after.endPinned)
                    assertEquals("$label 예고=결과", after.period, preview.thisCycle)
                    val previous = dao.rows.value.lastOrNull { LocalDate.parse(it.start).isBefore(boundary) }
                    assertEquals("$label 지난 회차 끝", boundary.toString(), previous?.endExclusive)
                    assertEquals("$label 지난 회차 예고", previous?.let { BudgetCycle(LocalDate.parse(it.start), boundary) }, preview.previousCycle)
                    assertContiguous(dao, today)
                    cases++
                }
                boundary = boundary.plusDays(1)
            }
        }
        assertTrue("케이스가 너무 적음: $cases", cases > 200)
    }

    @Test
    fun `매트릭스 - changePayday(규칙 변경)는 모든 규칙 조합에서 시작을 유지하고 예고와 일치한다`() = runBlocking {
        for (from in paydays) for (to in paydays) {
            if (from == to) continue
            val (repo, dao, rc) = freshRepo(from)
            val current = repo.cycleAt(today)
            val preview = com.allowance.manager.core.domain.usecase.budget.PreviewPaydayChangeUseCase(repo, rc)(current.start, to)

            repo.changePayday(current.start, to, today)

            val after = repo.cycleAt(today)
            val label = "규칙 $from→$to"
            // 예고한 회차는 실제 행으로 저장돼야 한다
            val saved = dao.rows.value.first { it.start == current.start.toString() }
            assertEquals("$label 예고=저장", preview.thisCycle.endExclusive.toString(), saved.endExclusive)
            assertEquals("$label 새 규칙", to.coerceIn(0, 31), after.payday)
            assertTrue("$label 고정 아님", !after.endPinned)
            assertTrue("$label 끝은 오늘 뒤", after.endExclusive.isAfter(today))
            if (preview.nextStart.isAfter(today)) {
                // 새 규칙의 다음 지급일이 아직 안 왔으면 예고한 회차가 곧 이번 회차
                assertEquals("$label 이번 회차 = 예고", preview.thisCycle, after.period)
            } else {
                // 이미 지났으면(말일→10일을 12일에 바꿈 등) 관문이 굴려서 예고의 '다음 회차'가 이번 회차가 된다
                assertEquals("$label 이번 회차 = 예고의 다음 회차", preview.nextStart, after.start)
            }
            assertContiguous(dao, today)
        }
    }

    @Test
    fun `매트릭스 - setCycleEnd(받을 날)는 모든 규칙·날짜에서 끝을 고정하고 지나가면 규칙으로 복귀한다`() = runBlocking {
        for (payday in paydays) for (offset in 1L..40L) {
            val end = today.plusDays(offset)
            val (repo, dao, _) = freshRepo(payday)
            val current = repo.cycleAt(today)

            repo.setCycleEnd(current.start, end)

            val label = "규칙=$payday 끝=$end"
            val pinned = repo.cycleAt(today)
            assertEquals("$label 시작 유지", current.start, pinned.start)
            assertEquals("$label 끝 고정", end, pinned.endExclusive)
            assertTrue("$label 고정", pinned.endPinned)
            assertContiguous(dao, today)

            // 고정한 날이 되면 다음 회차가 거기서 시작하고 끝은 규칙으로
            val later = CycleRepositoryImpl(dao, FakeTransactionDao(), FakeRemoteConfigRepository()) { end }
            val next = later.cycleAt(end)
            assertEquals("$label 롤오버 시작", end, next.start)
            assertTrue("$label 롤오버 고정 아님", !next.endPinned)
            assertEquals("$label 롤오버 끝", com.allowance.manager.core.domain.model.BudgetCycle.endAfterPayDate(end, payday.coerceIn(0, 31)), next.endExclusive)
            assertContiguous(dao, end)
        }
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

// 페이크(FakeCycleDao·FakeTransactionDao·FakeRemoteConfigRepository)는 Fakes.kt — 회귀 테스트와 공유
