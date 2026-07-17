package com.allowance.manager.core.domain.usecase.store

import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.SpendingRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * 이번달 Room 거래 내역의 순잔액(입금 - 출금)을
 * 다음 월급날까지 남은 일수로 나눠 daily_allowance에 저장
 */
class CalculateDailyAllowanceUseCase @Inject constructor(
    private val spendingRepository: SpendingRepository,
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke() {
        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1)

        // 이번달 거래 내역만 필터링
        val thisMonthSpendings = spendingRepository.getAllSpendings().first().filter { spending ->
            val date = Instant.ofEpochMilli(spending.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            !date.isBefore(startOfMonth) && !date.isAfter(today)
        }

        // 순잔액 = 입금 합계 - 출금 합계
        val netAmount = thisMonthSpendings.sumOf { spending ->
            when (spending.type) {
                TransactionType.INCOME -> spending.amount
                TransactionType.SPEND -> -spending.amount
            }
        }

        // 다음 월급날까지 남은 일수
        val payday = dataStoreRepository.getNextPayday().first()
        val targetDate = today.withDayOfMonth(payday.toIntOrNull() ?: 25)
            .let { if (today.isAfter(it)) it.plusMonths(1) else it }
        val leftDays = ChronoUnit.DAYS.between(today, targetDate)

        if (leftDays <= 0) return

        val dailyAllowance = (netAmount / leftDays).coerceAtLeast(0L)
        dataStoreRepository.setDailyAllowance(dailyAllowance)
    }
}
