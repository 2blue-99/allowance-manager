package com.allowance.manager.core.domain.usecase.stats

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.usecase.FakeTransactionRepository
import com.allowance.manager.core.domain.usecase.d
import com.allowance.manager.core.domain.usecase.millis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** 통계 창의 사이클별 지출 합계 — 기간별 합계를 시작일 키로 묶는다 */
class ObserveCycleExpenseTotalsUseCaseTest {

    private val b = BudgetCycle(d(7, 24), d(8, 25))
    private val c = BudgetCycle(d(8, 25), d(9, 25))

    @Test
    fun `사이클마다 기간 합계를 시작일 키로 준다`() = runBlocking {
        val transactions = FakeTransactionRepository().apply {
            spentBetween = { start, _ ->
                when (start) {
                    d(7, 24).millis() -> 1_000L
                    d(8, 25).millis() -> 2_500L
                    else -> error("예상 밖 기간 $start")
                }
            }
        }

        val totals = ObserveCycleExpenseTotalsUseCase(transactions)(listOf(b, c)).first()

        assertEquals(mapOf(d(7, 24) to 1_000L, d(8, 25) to 2_500L), totals)
    }

    @Test
    fun `창이 비면 빈 맵`() = runBlocking {
        val totals = ObserveCycleExpenseTotalsUseCase(FakeTransactionRepository())(emptyList()).first()
        assertEquals(emptyMap<LocalDate, Long>(), totals)
    }
}
