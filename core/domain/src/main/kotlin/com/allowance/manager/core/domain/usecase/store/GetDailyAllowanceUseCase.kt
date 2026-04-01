package com.allowance.manager.core.domain.usecase.store

import com.allowance.manager.core.domain.model.Allowance
import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * 월 용돈에서 하루에 사용할 수 있는 금액
 */
class GetDailyAllowanceUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<Allowance> = combine(
        dataStoreRepository.getNexPayDay(),
        dataStoreRepository.getMonthAllowance()
    ) { payDay, monthAllowance ->
        val today = LocalDate.now()

        var targetDate = today.withDayOfMonth(payDay.toIntOrNull() ?: 25)

//            if (today.isAfter(targetDate)) {
//                targetDate = targetDate.plusMonths(1)
//            }

        val leftDay = ChronoUnit.DAYS.between(today, targetDate)

        Allowance(
            monthAllowance = monthAllowance,
            dailyAllowance = monthAllowance / leftDay,
            leftDays = leftDay
        )
    }
}
