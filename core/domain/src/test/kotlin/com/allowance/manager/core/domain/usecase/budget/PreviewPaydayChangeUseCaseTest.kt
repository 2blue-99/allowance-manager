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
 * 변경 미리보기 = 저장 결과. 저장 로직([CycleRepository.changePayday])과 같은 규칙을 따라야 한다:
 * 경계 이후 행은 대체, 직전 행 끝은 경계로, 새 회차 끝은 규칙일(보정)로.
 *
 * 2026년: 7/24(금, 25일 보정) · 8/25(화) · 9/25(금) · 9/30(수)
 */
class PreviewPaydayChangeUseCaseTest {

    private val rows = listOf(
        cycle(d(7, 24), d(8, 25)),
        cycle(d(8, 25), d(9, 25)),
    )
    private val useCase = PreviewPaydayChangeUseCase(FakeCycleRepository(rows), FakeRemoteConfigRepository())

    @Test
    fun `규칙만 변경 - 시작은 유지, 끝만 새 규칙으로`() = runBlocking {
        val p = useCase(boundary = d(8, 25), payday = 0)

        assertEquals(BudgetCycle(d(8, 25), d(9, 30)), p.thisCycle)
        assertEquals(d(9, 30), p.nextStart)
        // 직전 회차(7/24~)는 경계 8/25에서 그대로 끝난다
        assertEquals(BudgetCycle(d(7, 24), d(8, 25)), p.previousCycle)
    }

    @Test
    fun `경계를 사이클 중간으로 - 현재 회차가 경계에서 끊기고 새 회차가 시작`() = runBlocking {
        val p = useCase(boundary = d(9, 5), payday = 25)

        assertEquals(BudgetCycle(d(9, 5), d(9, 25)), p.thisCycle)
        assertEquals(BudgetCycle(d(8, 25), d(9, 5)), p.previousCycle)
    }

    @Test
    fun `경계를 이전 회차로 당기면 - 이전 회차 끝이 경계로 줄어든다`() = runBlocking {
        val p = useCase(boundary = d(8, 22), payday = 25)

        // 8/25는 3일 뒤라 건너뛰고 9/25
        assertEquals(BudgetCycle(d(8, 22), d(9, 25)), p.thisCycle)
        assertEquals(BudgetCycle(d(7, 24), d(8, 22)), p.previousCycle)
    }

    @Test
    fun `첫 회차보다 앞이면 직전 회차가 없다`() = runBlocking {
        val p = useCase(boundary = d(7, 1), payday = 25)

        assertEquals(d(7, 1), p.thisCycle.start)
        assertNull(p.previousCycle)
    }

    @Test
    fun `이직 시나리오 - 오늘 받았고 앞으로 15일`() = runBlocking {
        val p = useCase(boundary = d(9, 5), payday = 15)

        // 9/15은 열흘 뒤라 인정
        assertEquals(BudgetCycle(d(9, 5), d(9, 15)), p.thisCycle)
        assertEquals(BudgetCycle(d(8, 25), d(9, 5)), p.previousCycle)
    }
}
