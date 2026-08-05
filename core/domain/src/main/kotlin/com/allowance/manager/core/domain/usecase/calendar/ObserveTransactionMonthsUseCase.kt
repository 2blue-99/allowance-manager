package com.allowance.manager.core.domain.usecase.calendar

import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import javax.inject.Inject

/** 거래가 하나라도 있는 달 목록. 월 피커에서 데이터 있는 달만 선택 가능하게 하는 데 사용. */
class ObserveTransactionMonthsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(): Flow<List<YearMonth>> = transactionRepository.observeTransactionMonths()
}
