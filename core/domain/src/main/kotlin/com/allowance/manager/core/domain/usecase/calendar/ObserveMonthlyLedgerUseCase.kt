package com.allowance.manager.core.domain.usecase.calendar

import com.allowance.manager.core.domain.model.MonthlyLedger
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * 가계부 위젯용 이번 달(달력 월: 1일~말일) 지출·수입 합계를 관찰.
 * 홈/예산과 달리 수급일 사이클이 아닌 달력 월 기준이다.
 */
class ObserveMonthlyLedgerUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(
        month: YearMonth = YearMonth.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Flow<MonthlyLedger> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return combine(
            transactionRepository.observeSpentBetween(start, end),
            transactionRepository.observeIncomeBetween(start, end),
        ) { expense, income -> MonthlyLedger(expense = expense, income = income) }
    }
}
