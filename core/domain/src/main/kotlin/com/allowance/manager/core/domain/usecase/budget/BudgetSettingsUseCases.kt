package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.MonthlyBudget
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.repository.BudgetRepository
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** 현재 사이클 용돈(이월 규칙 적용). 설정 화면 표시·현재 예산용. */
class GetMonthlyBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val observeCycleUseCase: ObserveCycleUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Long> =
        observeCycleUseCase().flatMapLatest { budgetRepository.observeBudgetForCycle(it.start) }
}

/** 용돈 변경 → 현재 사이클부터 적용되도록 이력에 저장(upsert). */
class SetMonthlyBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val getCycleUseCase: GetCycleUseCase,
) {
    suspend operator fun invoke(amount: Long) =
        budgetRepository.setBudgetForCycle(getCycleUseCase().start, amount)
}

/** 특정 사이클의 용돈(이월). 통계 계단식 점선·선택 사이클 요약용. */
class GetBudgetForCycleUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    suspend operator fun invoke(cycleStart: LocalDate): Long =
        budgetRepository.getBudgetForCycle(cycleStart)
}

/** 용돈 이력 전체 Flow. 통계 창에 매핑. */
class ObserveBudgetHistoryUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    operator fun invoke(): Flow<List<MonthlyBudget>> = budgetRepository.observeHistory()
}

class GetPaydayUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<Int> = dataStoreRepository.getPayday()
}

class SetPaydayUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(day: Int) = dataStoreRepository.setPayday(day)
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
