package com.allowance.manager.core.domain.usecase.store

import com.allowance.manager.core.domain.model.Allowance
import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetAllowanceInfoUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<Allowance> = combine(
        dataStoreRepository.getMonthAllowance(),
        dataStoreRepository.getDailyAllowance(),
        dataStoreRepository.getNextPayday(),
    ) { monthAllowance, dailyAllowance, payday ->
        val today = LocalDate.now()
        val targetDate = today.withDayOfMonth(payday.toIntOrNull() ?: 25)
            .let { if (today.isAfter(it)) it.plusMonths(1) else it }
        val leftDays = ChronoUnit.DAYS.between(today, targetDate)

        Allowance(
            monthAllowance = monthAllowance,
            dailyAllowance = dailyAllowance,
            leftDays = leftDays,
        )
    }
}
