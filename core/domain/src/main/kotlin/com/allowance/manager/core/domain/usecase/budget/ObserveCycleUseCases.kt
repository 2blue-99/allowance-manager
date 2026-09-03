package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.PaydayRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * [today]가 속한 사이클 — 홈·위젯·상태바·통계가 공유하는 **단일 진입점**.
 *
 * 이력([PaydayRepository])이 소스이고, 아직 이력이 없으면 DataStore의 규칙일로 계산한다.
 * 화면마다 사이클을 다시 만들지 말고 이 결과를 그대로 쓴다 — 어긋나면 홈과 위젯 값이 달라진다.
 */
class ObserveCycleUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    operator fun invoke(today: LocalDate = LocalDate.now()): Flow<BudgetCycle> =
        combine(
            paydayRepository.observeHistory(),
            dataStoreRepository.getPayday(),
        ) { rules, fallbackPayday ->
            BudgetCycle.of(
                rules = rules,
                today = today,
                holidays = remoteConfigRepository.getHolidays(),
                fallbackPayday = fallbackPayday,
            )
        }
}

/** [ObserveCycleUseCase]의 1회 조회판 — 알림 리시버 등 Flow가 필요 없는 곳에서 쓴다. */
class GetCycleUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): BudgetCycle =
        BudgetCycle.of(
            rules = paydayRepository.history(),
            today = today,
            holidays = remoteConfigRepository.getHolidays(),
            fallbackPayday = dataStoreRepository.getPayday().first(),
        )
}

/**
 * 현재 사이클의 **직전** 사이클들을 최신순으로 [count]개. 통계 6개월 창·월별 탐색용.
 *
 * 각 사이클의 시작 하루 전으로 되짚어 올라간다 — 경계가 맞물려 있으므로 빈틈이 생기지 않는다.
 */
class GetRecentCyclesUseCase @Inject constructor(
    private val getCycleUseCase: GetCycleUseCase,
    private val paydayRepository: PaydayRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    suspend operator fun invoke(count: Int, today: LocalDate = LocalDate.now()): List<BudgetCycle> {
        if (count <= 0) return emptyList()
        val rules = paydayRepository.history()
        val holidays = remoteConfigRepository.getHolidays()
        val fallbackPayday = dataStoreRepository.getPayday().first()

        val result = mutableListOf(getCycleUseCase(today))
        while (result.size < count) {
            val previousDay = result.last().start.minusDays(1)
            val previous = BudgetCycle.of(rules, previousDay, holidays, fallbackPayday)
            // 더 과거로 못 가면(이력의 첫 사이클) 중단
            if (previous.start == result.last().start) break
            result += previous
        }
        return result
    }
}
