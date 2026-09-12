package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.usecase.FakeCycleRepository
import com.allowance.manager.core.domain.usecase.FakeRemoteConfigRepository
import com.allowance.manager.core.domain.usecase.cycle
import com.allowance.manager.core.domain.usecase.d
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 시작 이동 예고 — 끝은 현재 회차 것을 유지해야 한다.
 * (규칙으로 끝을 다시 계산하면 말일 규칙에 8/20을 넣었을 때 8/20~8/30 같은 11일짜리 회차가 나온다)
 */
class PreviewCycleStartUseCaseTest {

    private val today = d(9, 12)
    private val rows = listOf(
        cycle(d(7, 31), d(8, 31), payday = 0, budget = 400_000L),
        cycle(d(8, 31), d(9, 30), payday = 0, budget = 500_000L),
    )
    private val useCase = PreviewCycleStartUseCase(FakeCycleRepository(rows), FakeRemoteConfigRepository())

    @Test
    fun `말일 규칙에 20 - 이번 회차는 8월 20일 ~ 9월 29일, 끝 유지`() = runBlocking {
        val p = useCase(boundary = d(8, 20), today = today)

        assertEquals(BudgetCycle(d(8, 20), d(9, 30)), p.thisCycle)
        assertEquals(BudgetCycle(d(7, 31), d(8, 20)), p.previousCycle)
    }

    @Test
    fun `회차 중간으로 옮기면 현재 회차가 지난 회차로 끊긴다`() = runBlocking {
        val p = useCase(boundary = d(9, 5), today = today)

        assertEquals(BudgetCycle(d(9, 5), d(9, 30)), p.thisCycle)
        assertEquals(BudgetCycle(d(8, 31), d(9, 5)), p.previousCycle)
    }

    @Test
    fun `첫 회차보다 앞이면 지난 회차가 없다`() = runBlocking {
        val p = useCase(boundary = d(7, 1), today = today)

        assertEquals(BudgetCycle(d(7, 1), d(9, 30)), p.thisCycle)
        assertNull(p.previousCycle)
    }
}
