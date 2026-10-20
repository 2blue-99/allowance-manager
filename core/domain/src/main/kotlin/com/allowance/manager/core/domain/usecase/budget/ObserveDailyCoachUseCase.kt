package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.DailyCoach
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * 오늘 코치 위젯용 지표(오늘 지출·오늘 권장·하루 평균)를 관찰.
 *
 * 권장·평균은 [ObserveBudgetStatusUseCase] 결과의 공용 계산(BudgetStatus.dailyRecommended/dailyAverage)을
 * 그대로 쓴다 — 홈 히어로 카드와 같은 값(수입 가산·100원 내림 포함). 오늘 지출만 날짜 범위로 따로 센다.
 */
class ObserveDailyCoachUseCase @Inject constructor(
    private val observeBudgetStatusUseCase: ObserveBudgetStatusUseCase,
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Flow<DailyCoach> {
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return combine(
            observeBudgetStatusUseCase(today),
            transactionRepository.observeBudgetSpentBetween(todayStart, todayEnd),
        ) { status, todaySpent ->
            DailyCoach(
                todaySpent = todaySpent,
                recommendedPerDay = status.dailyRecommended(today),
                avgPerDay = status.dailyAverage(today),
            )
        }
    }
}
