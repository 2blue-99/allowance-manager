package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** 현재 사이클 예산. 설정 화면 표시·현재 예산용. */
class GetMonthlyBudgetUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
) {
    operator fun invoke(): Flow<Long> =
        cycleRepository.observeCycleAt().map { it.budget }
}

/** 예산 변경 → 현재 사이클 행에 저장. 이후 새 사이클은 이 값을 이월한다. */
class SetMonthlyBudgetUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
) {
    suspend operator fun invoke(amount: Long) =
        cycleRepository.setBudget(cycleRepository.cycleAt().start, amount)
}

/** 특정 사이클의 예산 변경 — 디버그 시드 등 과거 사이클 조정용. */
class SetBudgetForCycleUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
) {
    suspend operator fun invoke(cycleStart: LocalDate, amount: Long) =
        cycleRepository.setBudget(cycleStart, amount)
}

/** 특정 사이클의 예산. 통계 선택 사이클 요약·알림 문구용. */
class GetBudgetForCycleUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
) {
    suspend operator fun invoke(cycleStart: LocalDate): Long =
        cycleRepository.budgetFor(cycleStart)
}

/** 현재 규칙일(1~31, 0=말일) — 최신 사이클 행의 payday가 단일 소스. 설정 화면 표시용. */
class GetPaydayUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
) {
    operator fun invoke(): Flow<Int> = cycleRepository.observeCycleAt().map { it.payday }
}

/** 사용자 유형(student/youth/common) 관찰 — 호칭 등 개인화에 사용 */
class GetUserTypeUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<UserType> = dataStoreRepository.getUserType()
}

/** 사용자 유형 저장 */
class SetUserTypeUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(type: UserType) = dataStoreRepository.setUserType(type)
}
