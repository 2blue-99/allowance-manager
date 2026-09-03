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
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import com.allowance.manager.core.domain.usecase.alert.PaydayNoticeDecider
import com.allowance.manager.core.domain.usecase.config.FetchRemoteConfigUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetStatusUseCase
import com.allowance.manager.core.domain.usecase.budget.GetCycleUseCase
import com.allowance.manager.core.domain.usecase.budget.SetPaydayUseCase
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.domain.usecase.setting.GetHomeGuideShownUseCase
import com.allowance.manager.core.domain.usecase.setting.SetHomeGuideShownUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
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
    private val getCycleUseCase: GetCycleUseCase,
    private val dataStoreRepository: DataStoreRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val fetchRemoteConfigUseCase: FetchRemoteConfigUseCase,
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
                budgetRepository.setBudgetForCycle(ym.atDay(1), budget)

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
     * 테스트용: 선택한 달에 [amount]원 지출 1건 추가. 누를 때마다 쌓인다.
     *
     * 날짜는 그 달 15일. 다만 이번 달이면 미래 거래가 되지 않도록 오늘 날짜로 넣는다.
     * 시:분:초는 지금 시각을 써서 같은 달에 여러 건을 넣어도 정렬이 구분된다.
     */
    fun addExpenseToMonth(month: YearMonth, amount: Long) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val day = if (month == YearMonth.now()) today.dayOfMonth else 15
            val now = LocalTime.now()
            val createdAt = month.atDay(day)
                .atTime(now.hour, now.minute, now.second)
                .atZone(zone).toInstant().toEpochMilli()
            transactionRepository.record(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    packageName = "",
                    sourceName = "테스트 지출",
                    isManual = true,
                    createdAt = createdAt,
                ),
            )
        }
    }

    /**
     * 테스트용: 그 달에 걸친 사이클의 예산을 [amount]원으로 설정.
     *
     * 예산 키는 사이클 시작일이라, 달 하나를 받아 그 달 중간이 속한 사이클을 찾아 저장한다.
     * (그 사이클부터 적용되고, 이후 사이클은 바꾸지 않는 한 이 값을 이월한다)
     */
    fun setBudgetForMonth(month: YearMonth, amount: Long) {
        viewModelScope.launch {
            val cycle = getCycleUseCase(month.atDay(15))
            budgetRepository.setBudgetForCycle(cycle.start, amount)
        }
    }

    /** Remote Config 표시를 다시 읽게 하는 트리거(원격 값은 Flow가 아니라 동기 읽기) */
    private val configRefresh = MutableStateFlow(0)

    /**
     * Remote Config 현재 값 — 강제 업데이트 버전·업데이트 노트·공휴일 데이터.
     *
     * 공휴일은 version이 0이면 원격을 못 받아 **내장 폴백**을 쓰는 중이라는 뜻이다.
     * 실기기에서 "Remote Config가 실제로 붙었는지"를 이걸로 구분한다.
     */
    val remoteConfigText: StateFlow<String> = configRefresh
        .map {
            val holidays = remoteConfigRepository.getHolidays()
            val source = if (holidays.version == 0) "내장 폴백" else "원격 v${holidays.version}"
            val upcoming = holidays.byDate.entries
                .filter { e -> !e.key.isBefore(LocalDate.now()) }
                .sortedBy { e -> e.key }
                .take(3)
                .joinToString(", ") { e -> "${e.key.monthValue}/${e.key.dayOfMonth} ${e.value}" }
                .ifEmpty { "없음" }
            buildString {
                appendLine("공휴일 $source · ${holidays.count}건")
                appendLine("다가오는: $upcoming")
                appendLine("강제 업데이트 버전: ${remoteConfigRepository.getForcedUpdateVersion().ifEmpty { "(없음)" }}")
                append("추천 업데이트 버전: ${remoteConfigRepository.getRecommendUpdateVersion().ifEmpty { "(없음)" }}")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "…")

    /** Remote Config를 지금 fetch·활성화하고 표시를 갱신. (최소 fetch 간격 때문에 값이 안 바뀔 수 있다) */
    fun fetchRemoteConfig() {
        viewModelScope.launch {
            runCatching { fetchRemoteConfigUseCase() }
            configRefresh.value++
            paydayRefresh.value++
        }
    }

    /** 월급일 알림 디버그 텍스트를 다시 읽게 하는 트리거(발송 표식은 Flow가 아니라 1회성 읽기) */
    private val paydayRefresh = MutableStateFlow(0)

    /**
     * 월급일 알림 상태 — 규칙일·조정 여부·실지급일·오늘 판정·마지막 발송 표식.
     * 실지급일은 사이클 경계와 같은 계산에서 나오므로, 이 값이 홈 D-day와 다르면 버그다.
     */
    val paydayDebugText: StateFlow<String> =
        combine(dataStoreRepository.getPayday(), paydayRefresh) { payday, _ -> payday }
            .map { payday ->
                val today = LocalDate.now()
                val holidays = remoteConfigRepository.getHolidays()
                val actual = BudgetCycle.payDate(YearMonth.from(today), payday, holidays)
                val notice = PaydayNoticeDecider.decide(today = today, payday = payday, holidays = holidays)
                val lastSent = dataStoreRepository.getPaydayAlertLastSent().ifEmpty { "없음" }
                val ruleLabel = if (payday <= 0) "말일" else "${payday}일"
                buildString {
                    appendLine("규칙 $ruleLabel")
                    appendLine("실지급일 ${actual.dayLabel()} · 오늘 ${today.dayLabel()}")
                    append("판정 $notice · 마지막 발송 $lastSent")
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "…")

    private fun LocalDate.dayLabel(): String =
        "${monthValue}월 ${dayOfMonth}일(${dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"

    /**
     * 테스트용(월급일 알림): 규칙일을 오늘 + [offsetDays]의 일(日)로 바꾼다.
     * 0 = 오늘(당일 알림 확인), 1 = 내일(전날 알림 확인). 영업일 보정은 그대로 걸린다.
     */
    fun setPaydayForTest(offsetDays: Long) {
        viewModelScope.launch {
            dataStoreRepository.setPayday(LocalDate.now().plusDays(offsetDays).dayOfMonth)
            paydayRefresh.value++
        }
    }

    /**
     * 테스트용: 월급일 알림 리시버를 지금 즉시 발화.
     * [force]가 null이면 실제 경로(설정·판정·중복검사)를 그대로 탄다 — 오늘이 지급일 전날/당일이 아니면 아무것도 안 뜬다.
     * 값을 주면 판정·중복검사를 건너뛰고 그 문구만 보낸다(발송 표식도 남기지 않음).
     * (app 모듈 클래스를 직접 참조할 수 없어 명시적 ComponentName으로 브로드캐스트)
     */
    fun triggerPaydayAlertNow(force: PaydayNoticeDecider.Notice? = null) {
        val intent = Intent("com.allowance.manager.action.PAYDAY_ALERT")
            .setComponent(
                ComponentName(appContext.packageName, "com.allowance.manager.service.PaydayAlarmReceiver"),
            )
        if (force != null) intent.putExtra("force_notice", force.name)
        appContext.sendBroadcast(intent)
        paydayRefresh.value++
    }

    /** 테스트용: 중복 발송 방지 표식을 지워 같은 날 다시 실제 경로로 발송할 수 있게 한다. */
    fun clearPaydayAlertSentMark() {
        viewModelScope.launch {
            dataStoreRepository.setPaydayAlertLastSent("")
            paydayRefresh.value++
        }
    }

    /**
     * 테스트용(예산 소진 알림): 이번 달 예산의 10%를 지금 소진하는 지출 1건 삽입.
     * 누를 때마다 남은 비율이 10%p씩 떨어져 임계값(90/70/50/30/10/0…)을 순서대로 관통 → 알림 확인.
     */
    fun spendBudgetTenPercent() {
        viewModelScope.launch {
            val budget = budgetRepository.getBudgetForCycle(getCycleUseCase().start)
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
