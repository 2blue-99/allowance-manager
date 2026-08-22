package com.allowance.manager.feature.setting

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.common.BaseViewModel
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.repository.BudgetRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetStatusUseCase
import com.allowance.manager.core.domain.usecase.budget.SetPaydayUseCase
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.domain.usecase.setting.GetHomeGuideShownUseCase
import com.allowance.manager.core.domain.usecase.setting.SetHomeGuideShownUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class DebugViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    getHomeGuideShownUseCase: GetHomeGuideShownUseCase,
    observeBudgetStatusUseCase: ObserveBudgetStatusUseCase,
    private val setHomeGuideShownUseCase: SetHomeGuideShownUseCase,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val setPaydayUseCase: SetPaydayUseCase,
) : BaseViewModel() {

    /** 예산 소진 알림 테스트용 지출의 출처명(되돌리기 시 이 값으로 필터해 삭제) */
    private val debugSource = "[디버그] 예산 10% 소진"

    /** 현재 소진/남은 상태 텍스트 — 10% 소진·되돌리기 때마다 실시간 갱신 */
    val budgetStatusText: StateFlow<String> = observeBudgetStatusUseCase()
        .map { status ->
            if (status.budget <= 0) {
                "예산 미설정"
            } else {
                val remainingPct = (status.remaining * 100f / status.budget).roundToInt()
                "소진 ${100 - remainingPct}% · 남은 ${remainingPct}% " +
                    "(${status.remaining.amountToComma()} / ${status.budget.amountToComma()}원)"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "…")

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

    /**
     * 테스트용(예산 소진 알림): 이번 달 예산의 10%를 지금 소진하는 지출 1건 삽입.
     * 누를 때마다 남은 비율이 10%p씩 떨어져 임계값(90/70/50/30/10/0…)을 순서대로 관통 → 알림 확인.
     */
    fun spendBudgetTenPercent() {
        viewModelScope.launch {
            val budget = budgetRepository.observeBudgetForMonth(YearMonth.now()).first()
            if (budget <= 0) return@launch
            transactionRepository.record(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = budget / 10,
                    packageName = "",
                    sourceName = debugSource,
                    isManual = true,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /** 되돌리기: 이 화면에서 넣은 "예산 10% 소진" 지출을 전부 삭제해 원래 잔액으로 복구. */
    fun resetDebugSpending() {
        viewModelScope.launch {
            transactionRepository.observeAll().first()
                .filter { it.sourceName == debugSource }
                .forEach { transactionRepository.delete(it.id) }
        }
    }

    /**
     * 테스트용(가계부 관리 알림): 리마인더 리시버를 지금 즉시 발화.
     * 실제 경로(안 본 내역 판정 포함)를 그대로 탄다 → "예산 10% 소진" 등으로 새 내역을 만든 뒤 눌러야 알림이 뜬다.
     * (app 모듈 클래스를 직접 참조할 수 없어 명시적 ComponentName으로 브로드캐스트)
     */
    fun triggerDailyReminderNow() {
        val intent = Intent("com.allowance.manager.action.DAILY_REMINDER")
            .setComponent(
                ComponentName(appContext.packageName, "com.allowance.manager.service.DailyReminderReceiver"),
            )
        appContext.sendBroadcast(intent)
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
