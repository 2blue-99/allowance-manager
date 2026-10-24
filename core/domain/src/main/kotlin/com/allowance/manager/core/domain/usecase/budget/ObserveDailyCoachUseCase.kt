package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.DailyCoach
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * 하루 지표(금일 지출 · 하루 지출 권장 · 월급일까지)를 관찰. 홈 예산 카드와 오늘 코치 위젯이 공유한다.
 *
 * 권장액은 [BudgetStatus.dailyRecommended] 로 **오늘 0시 기준 고정** — 오늘 지출·수입을 되돌린 잔액으로
 * 계산하므로 하루 중 거래가 들어와도 바뀌지 않고, 날짜가 바뀔 때만 바뀐다.
 * [today]는 구독 시점에 고정 — 오래 사는 구독은 todayTicks 로 자정마다 재구독할 것.
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
            transactionRepository.observeBudgetIncomeBetween(todayStart, todayEnd),
        ) { status, todaySpent, todayIncome ->
            DailyCoach(
                todaySpent = todaySpent,
                recommendedPerDay = status.dailyRecommended(today, todaySpent, todayIncome),
                daysUntilPayday = status.daysUntilPayday(today),
            )
        }
    }
}
