package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.DailyCoach
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.max

/**
 * 오늘 코치 위젯용 지표(오늘 지출·오늘 권장·하루 평균)를 관찰.
 * 예산·지출 모두 현재 사이클 기준이고, 오늘 지출만 날짜 범위로 따로 센다.
 */
class ObserveDailyCoachUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val transactionRepository: TransactionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Flow<DailyCoach> =
        cycleRepository.observeCycleAt(today)
            .flatMapLatest { cycle ->
                val period = cycle.period
                val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
                val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

                combine(
                    transactionRepository.observeBudgetSpentBetween(period.startMillis(zone), period.endMillis(zone)),
                    transactionRepository.observeBudgetSpentBetween(todayStart, todayEnd),
                ) { cycleSpent, todaySpent ->
                    val remaining = cycle.budget - cycleSpent
                    // 오늘 포함, 다음 수급일까지 남은 일수 (최소 1)
                    val remainingDays = max(1L, ChronoUnit.DAYS.between(today, cycle.endExclusive))
                    // 사이클 시작일부터 오늘까지 경과 일수 (오늘 포함, 최소 1)
                    val elapsedDays = max(1L, ChronoUnit.DAYS.between(cycle.start, today) + 1)

                    DailyCoach(
                        todaySpent = todaySpent,
                        recommendedPerDay = if (remaining > 0) remaining / remainingDays else 0L,
                        avgPerDay = cycleSpent / elapsedDays,
                    )
                }
            }
}
