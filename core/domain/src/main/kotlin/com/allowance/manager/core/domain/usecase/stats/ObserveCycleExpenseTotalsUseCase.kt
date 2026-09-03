package com.allowance.manager.core.domain.usecase.stats

import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * 사이클별 예산 지출 합계 (사이클 시작일 → 금액).
 *
 * 거래에 박아둔 `cycle_start`로 묶으므로 `GROUP BY` 한 번에 끝난다.
 * (사이클 경계는 사용자마다·달마다 달라 SQL이 스스로 계산할 수 없다)
 */
class ObserveCycleExpenseTotalsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(): Flow<Map<LocalDate, Long>> =
        transactionRepository.observeCycleExpenseTotals()
}
