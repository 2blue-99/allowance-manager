package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.usecase.budget.ObserveCyclesUseCase
import com.allowance.manager.core.domain.usecase.calendar.GetAdjacentCycleUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveTransactionCyclesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 사이클 테이블 리팩터링 회귀 — 실제 저장소 + 화면이 쓰는 UseCase 조합으로 화면 동작을 재현한다.
 *
 * 배경: 이웃 사이클 이동을 행 기반으로 바꾸면서 첫 행에서 멈추게 돼, 새 사용자(행 1개)의 통계 창이
 * 6칸이 아니라 1칸으로 그려졌다. 저장소 조회(첫 행 이전 = 규칙 역산 가상 회차)와 어긋난 것이 원인.
 * 규칙일 후보 전체에 대해 "화면이 실제로 하는 계산"을 그대로 돌려 같은 종류의 회귀를 막는다.
 */
class CycleConsumersRegressionTest {

    private val today = LocalDate.of(2026, 9, 12)
    private val paydays = listOf(0, 1, 5, 10, 15, 20, 25, 28, 31)

    private class Env(payday: Int, today: LocalDate) {
        val dao = FakeCycleDao()
        val tx = FakeTransactionDao()
        val rc = FakeRemoteConfigRepository()
        val repo = CycleRepositoryImpl(dao, tx, rc) { today }
        val adjacent = GetAdjacentCycleUseCase(repo, rc)
        val transactionCycles = ObserveTransactionCyclesUseCase(repo, TransactionRepositoryImpl(tx))
        val cycles = ObserveCyclesUseCase(repo)

        init { runBlocking { repo.init(payday, today) } }

        /** StatsViewModel.moveWindowTo 와 같은 알고리즘 — 끝에서 과거로 6칸 */
        suspend fun window(end: BudgetCycle, size: Int = 6): List<BudgetCycle> {
            val list = mutableListOf(end)
            repeat(size - 1) {
                val prev = adjacent(list.first(), -1)
                if (prev.start == list.first().start) return@repeat
                list.add(0, prev)
            }
            return list
        }
    }

    private fun assertChained(label: String, window: List<BudgetCycle>) {
        window.zipWithNext().forEach { (a, b) -> assertEquals("$label 맞물림 ${a.endExclusive}/${b.start}", a.endExclusive, b.start) }
        assertEquals("$label 중복 없음", window.size, window.distinctBy { it.start }.size)
    }

    // ─────────────────────────── 통계 창 ───────────────────────────

    @Test
    fun `새 사용자 - 행 하나·거래 없음이어도 통계 창은 6칸이고 현재 회차만 선택 가능`() = runBlocking {
        for (payday in paydays) {
            val env = Env(payday, today)
            val current = env.repo.cycleAt(today).period
            val label = "규칙=$payday"

            val window = env.window(current)

            assertEquals("$label 창 크기", 6, window.size)
            assertEquals("$label 마지막 = 현재", current, window.last())
            assertChained(label, window)
            // 가상 회차는 저장하지 않는다 — 행은 여전히 1개
            assertEquals("$label 행 수", 1, env.dao.rows.value.size)
            // 거래 있는 회차 없음 → 선택 가능한 막대는 현재(todayCycle) 하나
            assertEquals("$label 거래 회차", emptyList<BudgetCycle>(), env.transactionCycles().first())
            // 가상 회차 예산은 행이 없으니 0 (통계 점선)
            val budgets = env.cycles().first().associate { it.start to it.budget }
            window.dropLast(1).forEach { assertEquals("$label 가상 회차 예산", null, budgets[it.start]) }
        }
    }

    @Test
    fun `첫 거래를 현재 회차에 넣어도 창은 6칸, 거래 있는 회차는 현재 하나`() = runBlocking {
        for (payday in paydays) {
            val env = Env(payday, today)
            val current = env.repo.cycleAt(today).period
            env.tx.add(today)

            val window = env.window(env.repo.cycleAt(today).period)

            assertEquals("규칙=$payday 창 크기", 6, window.size)
            assertEquals("규칙=$payday 거래 회차", listOf(current), env.transactionCycles().first())
            assertEquals("규칙=$payday 행 수", 1, env.dao.rows.value.size)   // 현재 회차 안이라 백필 없음
        }
    }

    @Test
    fun `과거 거래가 들어오면 백필 행이 생기고, 창은 여전히 6칸, 백필 예산은 0`() = runBlocking {
        for (payday in paydays) {
            val env = Env(payday, today)
            val current = env.repo.cycleAt(today).period
            env.tx.add(current.start.minusDays(45))   // 두 회차 전쯤

            val window = env.window(env.repo.cycleAt(today).period)
            val rows = env.cycles().first()

            val label = "규칙=$payday"
            assertEquals("$label 창 크기", 6, window.size)
            assertChained(label, window)
            assertTrue("$label 백필 행 생성", rows.size >= 2)
            assertTrue("$label 첫 행이 거래를 덮음", !rows.first().start.isAfter(current.start.minusDays(45)))
            rows.dropLast(1).forEach { assertEquals("$label 백필 예산 0", 0L, it.budget) }
            // 거래 있는 회차 = 거래를 덮는 백필 행 하나
            val dataCycles = env.transactionCycles().first()
            assertEquals("$label 거래 회차 수", 1, dataCycles.size)
            // 창 안의 회차와 행이 같은 경계를 쓴다 (통계 막대 ↔ 행 예산 매핑이 어긋나지 않게)
            rows.forEach { row -> assertTrue("$label 행이 창 경계와 일치 ${row.start}", window.any { it.start == row.start } || row.start.isBefore(window.first().start)) }
        }
    }

    @Test
    fun `통계 창을 6칸씩 과거로 넘겨도 맞물리고, 되돌아오면 제자리`() = runBlocking {
        for (payday in paydays) {
            val env = Env(payday, today)
            val current = env.repo.cycleAt(today).period

            // StatsViewModel.moveWindow(-6) 와 같음: 끝을 6칸 뒤로 옮긴 뒤 창을 다시 채운다
            val olderEnd = env.adjacent(current, -6)
            val older = env.window(olderEnd)
            assertEquals("규칙=$payday 이전 창", 6, older.size)
            assertChained("규칙=$payday 이전 창", older)
            assertEquals("규칙=$payday 이전 창 끝", olderEnd, older.last())
            // 이전 창의 끝 다음 회차 = 현재 창의 첫 회차
            assertEquals("규칙=$payday 창 연결", env.window(current).first(), env.adjacent(olderEnd, +1))

            val back = env.adjacent(olderEnd, +6)
            assertEquals("규칙=$payday 왕복", current, back)
        }
    }

    // ─────────────────────────── 월별 탭 이동 ───────────────────────────

    @Test
    fun `월별 - 거래 있는 가장 오래된 회차까지만 이전으로 가고, 현재에서 다음은 멈춘다`() = runBlocking {
        for (payday in paydays) {
            val env = Env(payday, today)
            val current = env.repo.cycleAt(today).period
            env.tx.add(current.start.minusDays(10))   // 직전 회차에 거래 1건

            val dataCycles = env.transactionCycles().first()
            val oldest = dataCycles.minByOrNull { it.start }!!
            // CalendarViewModel.canGoPrev: cycle.start > oldest.start
            var cycle = env.repo.cycleAt(today).period
            var steps = 0
            while (cycle.start.isAfter(oldest.start)) { cycle = env.adjacent(cycle, -1); steps++ }
            assertEquals("규칙=$payday 이전 한 칸", 1, steps)
            assertEquals("규칙=$payday 가장 오래된 거래 회차 도달", oldest, cycle)
            // 현재에서 다음은 시작일이 같아 정지로 판정
            assertEquals("규칙=$payday 다음 멈춤", current.start, env.adjacent(current, +1).start)
        }
    }
}
