package com.allowance.manager.feature.stats

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.Cycle
import com.allowance.manager.core.domain.usecase.budget.GetUserTypeUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveCycleUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveCyclesUseCase
import com.allowance.manager.core.domain.usecase.calendar.GetAdjacentCycleUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveCycleTransactionsUseCase
import com.allowance.manager.core.domain.usecase.calendar.ObserveTransactionCyclesUseCase
import com.allowance.manager.core.domain.usecase.stats.ObserveCycleExpenseTotalsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * 통계 ViewModel 회귀 — 화면이 쓰는 UseCase를 진짜로 조립하고 저장소만 페이크.
 *
 * 배경: 사이클 테이블 리팩터링 뒤 새 사용자(행 1개·거래 없음)의 통계 창이 6칸이 아니라 1칸으로 그려져
 * 막대 하나가 폭을 다 차지했다. 이 테스트는 그 증상을 ViewModel 출력(uiState.window)에서 직접 잡는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val today = LocalDate.of(2026, 9, 12)
    private val paydays = listOf(0, 1, 5, 10, 15, 20, 25, 28, 31)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun LocalDate.millis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun viewModel(cycles: FakeCycleRepository, transactions: FakeTransactionRepository): StatsViewModel {
        val remoteConfig = FakeRemoteConfigRepository()
        return StatsViewModel(
            observeCycleExpenseTotalsUseCase = ObserveCycleExpenseTotalsUseCase(transactions),
            observeCyclesUseCase = ObserveCyclesUseCase(cycles),
            getUserTypeUseCase = GetUserTypeUseCase(FakeDataStoreRepository()),
            observeTransactionCyclesUseCase = ObserveTransactionCyclesUseCase(cycles, transactions),
            observeCycleTransactionsUseCase = ObserveCycleTransactionsUseCase(transactions),
            observeCycleUseCase = ObserveCycleUseCase(cycles),
            getAdjacentCycleUseCase = GetAdjacentCycleUseCase(cycles, remoteConfig),
            analytics = FakeAnalyticsHelper(),
        )
    }

    /** 규칙 [payday]로 온보딩 직후 상태 — 행 1개(현재 회차), 예산 50만 */
    private fun newUserCycles(payday: Int): Pair<FakeCycleRepository, BudgetCycle> {
        val current = BudgetCycle.of(payday, today)
        return FakeCycleRepository(listOf(Cycle(current.start, current.endExclusive, 500_000L, payday))) to current
    }

    @Test
    fun `새 사용자 - 행 하나·거래 없음이어도 막대는 6개, 현재 회차만 선택 가능`() = runTest {
        for (payday in paydays) {
            val (cycles, current) = newUserCycles(payday)
            val vm = viewModel(cycles, FakeTransactionRepository())
            val state = vm.uiState.value
            val label = "규칙=$payday"

            assertEquals("$label 막대 수", 6, state.window.size)
            assertEquals("$label 마지막 막대 = 현재", current, state.window.last().cycle)
            assertEquals("$label 선택 = 현재", current, state.selected)
            state.window.zipWithNext().forEach { (a, b) -> assertEquals("$label 맞물림", a.cycle.endExclusive, b.cycle.start) }
            assertEquals("$label 선택 가능 = 현재만", listOf(false, false, false, false, false, true), state.window.map { it.exists })
            assertEquals("$label 지출 전부 0", 0L, state.window.sumOf { it.expense })
            // 예산 점선: 행 있는 현재만 50만, 가상 과거는 0
            assertEquals("$label 현재 예산", 500_000L, state.window.last().budget)
            assertTrue("$label 과거 예산 0", state.window.dropLast(1).all { it.budget == 0L })
            assertFalse("$label 이전으로 못 감(거래 없음)", state.canOlder)
            assertFalse("$label 다음으로 못 감(현재)", state.canNewer)
            assertFalse("$label 로딩 끝", state.isLoading)
        }
    }

    @Test
    fun `첫 거래를 넣어도 막대는 6개, 그 막대만 채워지고 선택 가능`() = runTest {
        for (payday in paydays) {
            val (cycles, current) = newUserCycles(payday)
            val transactions = FakeTransactionRepository().apply {
                allTimes = listOf(today.millis())
                spentBetween = { start, _ -> if (start == current.startMillis()) 12_000L else 0L }
            }
            val vm = viewModel(cycles, transactions)
            val state = vm.uiState.value
            val label = "규칙=$payday"

            assertEquals("$label 막대 수", 6, state.window.size)
            assertEquals("$label 현재 막대 지출", 12_000L, state.window.last().expense)
            assertEquals("$label 나머지 지출 0", 0L, state.window.dropLast(1).sumOf { it.expense })
            assertTrue("$label 현재만 선택 가능", state.window.last().exists && state.window.dropLast(1).none { it.exists })
            assertEquals("$label 요약 지출", 0L, state.summary.expense)   // 선택 회차 내역 목록은 페이크가 빈 목록 → 요약 0
            assertFalse("$label 거래가 현재 회차뿐이면 이전으로 못 감", state.canOlder)
        }
    }

    @Test
    fun `직전 회차 거래는 현재 창 안이라 이전 페이지가 없다`() = runTest {
        val payday = 25
        val current = BudgetCycle.of(payday, today)
        val previous = BudgetCycle(BudgetCycle.previousPayDateBefore(current.start, payday), current.start)
        val cycles = FakeCycleRepository(
            listOf(
                Cycle(previous.start, previous.endExclusive, 0L, payday),
                Cycle(current.start, current.endExclusive, 500_000L, payday),
            ),
        )
        val transactions = FakeTransactionRepository().apply { allTimes = listOf(previous.start.plusDays(3).millis()) }
        val state = viewModel(cycles, transactions).uiState.value

        assertEquals(6, state.window.size)
        assertTrue("직전 막대가 선택 가능", state.window[4].exists)
        assertFalse("창 안에 다 들어오므로 이전 페이지 없음", state.canOlder)
    }

    @Test
    fun `창 밖(7회차 전)에 거래가 있으면 이전 페이지로 갈 수 있고, 넘긴 창도 맞물린다`() = runTest {
        val payday = 25
        val current = BudgetCycle.of(payday, today)
        // 현재 + 과거 7회차 = 행 8개 (저장소 백필이 만들어주는 모양)
        val chain = mutableListOf(current)
        repeat(7) {
            val start = BudgetCycle.previousPayDateBefore(chain.first().start, payday)
            chain.add(0, BudgetCycle(start, chain.first().start))
        }
        val oldest = chain.first()
        val cycles = FakeCycleRepository(chain.map { Cycle(it.start, it.endExclusive, if (it == current) 500_000L else 0L, payday) })
        val transactions = FakeTransactionRepository().apply { allTimes = listOf(oldest.start.plusDays(3).millis()) }
        val vm = viewModel(cycles, transactions)

        assertTrue("창 밖 과거에 거래가 있으면 이전 페이지 가능", vm.uiState.value.canOlder)
        val firstOfCurrentWindow = vm.uiState.value.window.first().cycle

        vm.onOlder()
        val older = vm.uiState.value
        assertEquals("이전 창도 6칸", 6, older.window.size)
        older.window.zipWithNext().forEach { (a, b) -> assertEquals("이전 창 맞물림", a.cycle.endExclusive, b.cycle.start) }
        // 이전 창의 마지막 칸은 현재 창 첫 칸의 직전 회차 — 두 창이 빈틈 없이 이어진다
        assertEquals("창 연결", firstOfCurrentWindow.start, older.window.last().cycle.endExclusive)
        assertTrue("거래 있는 7회차 전 막대가 이전 창에 있고 선택 가능", older.window.any { it.cycle == oldest && it.exists })
        assertFalse("그보다 더 과거는 거래가 없어 못 감", older.canOlder)
        assertTrue("이전 창에서는 다음으로 갈 수 있음", older.canNewer)

        vm.onNewer()
        assertEquals("되돌아오면 현재 창", current, vm.uiState.value.window.last().cycle)
    }
}
