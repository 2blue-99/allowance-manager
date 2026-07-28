package com.allowance.manager.core.domain.usecase.calendar

import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * 지정한 '달력 월'(YearMonth)에 속한 입출금 내역을 최신순으로 관찰.
 * 예산 사이클과 무관하게 순수 달력 기준으로 조회한다. (보이는 달만 쿼리)
 */
class ObserveMonthTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<Transaction>> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return transactionRepository.observeBetween(start, end)
    }
}
