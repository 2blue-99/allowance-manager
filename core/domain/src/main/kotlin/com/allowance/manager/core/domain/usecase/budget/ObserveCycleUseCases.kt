package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.Cycle
import com.allowance.manager.core.domain.repository.CycleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * [today]가 속한 사이클(기간) — 홈·위젯·상태바·월별·통계가 공유하는 **단일 진입점**.
 *
 * 소스는 `cycles` 테이블이고, 필요한 행은 저장소 관문이 자동 생성한다.
 * 화면마다 사이클을 다시 만들지 말고 이 결과를 그대로 쓴다 — 어긋나면 홈과 위젯 값이 달라진다.
 */
class ObserveCycleUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
) {
    operator fun invoke(today: LocalDate = LocalDate.now()): Flow<BudgetCycle> =
        cycleRepository.observeCycleAt(today).map { it.period }
}

/** [ObserveCycleUseCase]의 1회 조회판 — 알림 리시버 등 Flow가 필요 없는 곳에서 쓴다. */
class GetCycleUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): BudgetCycle =
        cycleRepository.cycleAt(today).period
}

/** 전체 사이클 행(오래된 → 최신) 관찰 — 통계의 사이클별 예산·창 계산용. */
class ObserveCyclesUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
) {
    operator fun invoke(): Flow<List<Cycle>> = cycleRepository.observeAll()
}
