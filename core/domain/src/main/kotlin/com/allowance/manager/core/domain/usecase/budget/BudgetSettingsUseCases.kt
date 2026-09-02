package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.MonthlyBudget
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.repository.BudgetRepository
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import javax.inject.Inject

/** 이번 달 용돈(이월 규칙 적용). 설정 화면 표시·현재 예산용. */
class GetMonthlyBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    operator fun invoke(): Flow<Long> = budgetRepository.observeBudgetForMonth(YearMonth.now())
}

/** 용돈 변경 → 이번 달부터 적용되도록 이력에 저장(upsert). */
class SetMonthlyBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    suspend operator fun invoke(amount: Long) =
        budgetRepository.setBudgetForMonth(YearMonth.now(), amount)
}

/** 특정 달의 용돈(이월). 통계 계단식 점선·선택 달 요약용. */
class GetBudgetForMonthUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    suspend operator fun invoke(month: YearMonth): Long =
        budgetRepository.getBudgetForMonth(month)
}

/** 용돈 이력 전체 Flow. 통계 6개월 창에 매핑. */
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
