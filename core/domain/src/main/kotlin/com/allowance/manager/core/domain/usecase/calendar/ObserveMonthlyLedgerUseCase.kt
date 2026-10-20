package com.allowance.manager.core.domain.usecase.calendar

import com.allowance.manager.core.domain.model.MonthlyLedger
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import javax.inject.Inject

/**
 * 가계부 위젯용 이번 달 지출·수입 합계를 관찰.
 * "이번 달" = [today]가 속한 **수급일 사이클** (홈·월별과 같은 범위). 사이클 경계가 바뀌면 재방출.
 */
class ObserveMonthlyLedgerUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val transactionRepository: TransactionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(today: LocalDate = LocalDate.now()): Flow<MonthlyLedger> =
        cycleRepository.observeCycleAt(today).flatMapLatest { cycle ->
            val period = cycle.period
            combine(
                transactionRepository.observeLedgerSpentBetween(period.startMillis(), period.endMillis()),
                transactionRepository.observeLedgerIncomeBetween(period.startMillis(), period.endMillis()),
            ) { expense, income -> MonthlyLedger(expense = expense, income = income) }
        }
}
