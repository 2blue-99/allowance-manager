package com.allowance.manager.feature.setting

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.common.BaseViewModel
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.repository.BudgetRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import com.allowance.manager.core.domain.usecase.budget.SetPaydayUseCase
import com.allowance.manager.core.domain.usecase.setting.GetHomeGuideShownUseCase
import com.allowance.manager.core.domain.usecase.setting.SetHomeGuideShownUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    getHomeGuideShownUseCase: GetHomeGuideShownUseCase,
    private val setHomeGuideShownUseCase: SetHomeGuideShownUseCase,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val setPaydayUseCase: SetPaydayUseCase,
) : BaseViewModel() {

    /** ON = 홈 가이드 표시(아직 안 본 상태). OFF = 표시 안 함(이미 본 것으로 처리). */
    val homeGuideEnabled: StateFlow<Boolean> =
        getHomeGuideShownUseCase()
            .map { shown -> !shown }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 토글 — ON이면 홈 재진입 시 가이드 노출, OFF면 노출 안 함 */
    fun setHomeGuideEnabled(enabled: Boolean) {
        viewModelScope.launch { setHomeGuideShownUseCase(!enabled) }
    }

    /**
     * 테스트용: 과거 12개월(직전 1~12개월 전) 각 달 15일에 수동 지출 1건씩 삽입.
     * + 각 달 용돈 upsert — 최근 6개월=50만, 그 이전 6개월=40/50 교대 (통계 계단식 점선 확인용).
     */
    fun seedTestData() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            for (monthsAgo in 1..12) {
                val ym = YearMonth.now().minusMonths(monthsAgo.toLong())
                // 용돈: 최근 6개월=50만, 그 이전 6개월(7~12전)=40/50 교대
                val budget = when {
                    monthsAgo <= 6 -> 500_000L
                    (monthsAgo - 7) % 2 == 0 -> 400_000L
                    else -> 500_000L
                }
                budgetRepository.setBudgetForMonth(ym, budget)

                val createdAt = ym.atDay(15).atStartOfDay(zone).toInstant().toEpochMilli()
                transactionRepository.record(
                    Transaction(
                        type = TransactionType.EXPENSE,
                        amount = 30_000L * monthsAgo,
                        packageName = "",
                        sourceName = "테스트 내역 ${monthsAgo}개월 전",
                        isManual = true,
                        createdAt = createdAt,
                    ),
                )
            }
        }
    }

    /**
     * 테스트용: 선택한 달에 값 세팅. null인 항목은 건너뜀.
     * - payday: 앱 전역 월급일(1~31, 0=말일)
     * - budget: 그 달부터 적용되는 용돈(upsert)
     * - expense/income: 그 달 15일에 해당 금액의 수동 거래 1건 삽입
     */
    fun applyMonthSettings(month: YearMonth, payday: Int?, budget: Long?, expense: Long?, income: Long?) {
        viewModelScope.launch {
            payday?.let { setPaydayUseCase(it) }
            budget?.let { budgetRepository.setBudgetForMonth(month, it) }

            val zone = ZoneId.systemDefault()
            val createdAt = month.atDay(15).atStartOfDay(zone).toInstant().toEpochMilli()
            if (expense != null && expense > 0) {
                transactionRepository.record(
                    Transaction(
                        type = TransactionType.EXPENSE,
                        amount = expense,
                        packageName = "",
                        sourceName = "지출 테스트",
                        isManual = true,
                        createdAt = createdAt,
                    ),
                )
            }
            if (income != null && income > 0) {
                transactionRepository.record(
                    Transaction(
                        type = TransactionType.INCOME,
                        amount = income,
                        packageName = "",
                        sourceName = "수입 테스트",
                        isManual = true,
                        createdAt = createdAt,
                    ),
                )
            }
        }
    }

    /** 테스트용: 선택한 달에 카테고리별 1건씩 삽입(수입 분류는 INCOME, 나머지는 EXPENSE). */
    fun seedCategoriesForMonth(month: YearMonth) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            TransactionCategory.entries.forEachIndexed { i, cat ->
                val createdAt = month.atDay((i % 27) + 1).atStartOfDay(zone).toInstant().toEpochMilli()
                transactionRepository.record(
                    Transaction(
                        type = if (cat == TransactionCategory.INCOME) TransactionType.INCOME else TransactionType.EXPENSE,
                        amount = (i + 1) * 5_000L,
                        packageName = "",
                        sourceName = "${cat.emoji} ${cat.label} 테스트",
                        category = cat,
                        isManual = true,
                        createdAt = createdAt,
                    ),
                )
            }
        }
    }
}
