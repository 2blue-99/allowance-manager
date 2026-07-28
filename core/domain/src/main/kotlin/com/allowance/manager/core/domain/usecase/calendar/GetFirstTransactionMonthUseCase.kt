package com.allowance.manager.core.domain.usecase.calendar

import com.allowance.manager.core.domain.repository.TransactionRepository
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * 가장 오래된 내역이 속한 달(YearMonth). 월별 화면에서 이보다 과거로는 이동하지 못하게 하는 하한.
 * 내역이 하나도 없으면 null.
 */
class GetFirstTransactionMonthUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(zone: ZoneId = ZoneId.systemDefault()): YearMonth? =
        transactionRepository.getFirstTransactionTime()?.let { millis ->
            val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            YearMonth.from(date)
        }
}
