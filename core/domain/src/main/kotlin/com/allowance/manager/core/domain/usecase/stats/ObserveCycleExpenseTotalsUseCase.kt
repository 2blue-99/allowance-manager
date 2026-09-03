package com.allowance.manager.core.domain.usecase.stats

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject

/**
 * 주어진 사이클들의 예산 지출 합계 (사이클 시작일 → 금액).
 *
 * 통계 창(6개)만큼의 사이클을 받아 기간별 합계를 묶는다.
 * 소속은 저장된 컬럼이 아니라 createdAt 날짜 범위로 계산되므로 경계를 바꿔도 자동으로 맞는다.
 */
class ObserveCycleExpenseTotalsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(cycles: List<BudgetCycle>): Flow<Map<LocalDate, Long>> {
        if (cycles.isEmpty()) return flowOf(emptyMap())
        val flows = cycles.map { cycle ->
            transactionRepository.observeBudgetSpentBetween(cycle.startMillis(), cycle.endMillis())
        }
        return combine(flows) { totals ->
            cycles.mapIndexed { i, cycle -> cycle.start to totals[i] }.toMap()
        }
    }
}
