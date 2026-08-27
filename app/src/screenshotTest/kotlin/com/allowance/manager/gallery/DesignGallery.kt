package com.allowance.manager.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.allowance.manager.core.designsystem.theme.AllowanceManagerTheme
import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.model.IgnoredAccount
import com.allowance.manager.core.domain.model.LedgerFilter
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.feature.account.AccountSettingScreen
import com.allowance.manager.feature.account.AccountUiState
import com.allowance.manager.feature.calendar.CalendarScreen
import com.allowance.manager.feature.calendar.CalendarUiState
import com.allowance.manager.feature.home.HomeScreen
import com.allowance.manager.feature.home.HomeUiState
import com.allowance.manager.feature.intro.IntroScreen
import com.allowance.manager.feature.onboarding.OnboardingAlertScreen
import com.allowance.manager.feature.onboarding.OnboardingInfoScreen
import com.allowance.manager.feature.onboarding.OnboardingPermissionScreen
import com.allowance.manager.feature.onboarding.OnboardingUiState
import com.allowance.manager.feature.setting.IgnoredAccountScreen
import com.allowance.manager.feature.setting.IgnoredAccountUiState
import com.allowance.manager.core.domain.model.Holidays
import com.allowance.manager.core.domain.model.PaydayInfo
import java.time.LocalDate
import com.allowance.manager.feature.setting.SettingScreen
import com.allowance.manager.feature.setting.SettingUiState
import com.allowance.manager.feature.splash.SplashScreen
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.feature.stats.CategorySlice
import com.allowance.manager.feature.stats.MonthBar
import com.allowance.manager.feature.stats.MonthSummary
import com.allowance.manager.feature.stats.StatsScreen
import com.allowance.manager.feature.stats.StatsUiState
import java.time.YearMonth

/**
 * 전체 화면을 한눈에 보기 위한 Preview 갤러리 (debug 전용, 앱에 포함되지 않음).
 *
 * Android Studio에서 이 파일을 열고 Split/Design 뷰로 전환하면
 * 모든 화면이 격자로 렌더링된다. 코드에서 직접 그리므로 토큰을 바꾸면 즉시 반영된다.
 */
private const val GROUP = "화면 전체"

// ── 샘플 데이터 ──────────────────────────────────────
private fun sampleTransaction(
    id: Long,
    source: String,
    amount: Long,
    type: TransactionType = TransactionType.EXPENSE,
    hidden: Boolean = false,
    accountId: Long? = 1L,
) = Transaction(
    id = id,
    type = type,
    amount = amount,
    balance = null,
    packageName = "com.sample.bank",
    sourceName = source,
    extractedAccount = "941602-**-***318",
    accountId = accountId,
    memo = null,
    scope = if (hidden) TxScope.EXCLUDED else TxScope.BUDGET,
    createdAt = 1_753_000_000_000L,
)

private val sampleHome = HomeUiState(
    budget = 2_000_000L,
    spent = 760_000L,
    income = 2_400_000L,
    remaining = 1_240_000L,
    ratio = 0.62f,
    spentRatio = 0.38f,
    isOver = false,
    dailyBudget = 103_000L,
    dailyAverage = 42_000L,
    overPace = false,
    daysUntilPayday = 12,
    transactions = listOf(
        sampleTransaction(1, "스타벅스", 5_600),
        sampleTransaction(2, "급여 입금", 2_400_000, TransactionType.INCOME),
        sampleTransaction(3, "네이버페이 충전", 10_000, hidden = true),
        sampleTransaction(4, "미등록 결제", 3_200, accountId = null),
    ),
    filter = LedgerFilter(showMain = false, showHidden = false),
    isLoading = false,
)

private val sampleStatsNow = YearMonth.now()
private fun sampleBar(back: Long, expense: Long, budget: Long, selected: Boolean = false): MonthBar {
    val ym = sampleStatsNow.minusMonths(back)
    return MonthBar(
        yearMonth = ym,
        label = "${ym.monthValue}월",
        expense = expense,
        budget = budget,
        isOver = budget > 0 && expense > budget,
        isSelected = selected,
        exists = true,
    )
}

private val sampleStats = StatsUiState(
    window = listOf(
        sampleBar(5, 720_000, 900_000),
        sampleBar(4, 950_000, 900_000),
        sampleBar(3, 880_000, 900_000),
        sampleBar(2, 840_000, 800_000),
        sampleBar(1, 1_040_000, 800_000),
        sampleBar(0, 760_000, 800_000, selected = true),
    ),
    selected = sampleStatsNow,
    summary = MonthSummary(budget = 800_000, expense = 760_000, income = 2_400_000),
    categories = listOf(
        CategorySlice(TransactionCategory.FOOD, 289_000, 0.38f),
        CategorySlice(TransactionCategory.CAFE, 137_000, 0.18f),
        CategorySlice(TransactionCategory.TRANSPORT, 91_000, 0.12f),
        CategorySlice(TransactionCategory.SHOPPING, 76_000, 0.10f),
        CategorySlice(TransactionCategory.LIVING, 61_000, 0.08f),
        CategorySlice(null, 106_000, 0.14f),
    ),
    canOlder = true,
    canNewer = false,
    isLoading = false,
)

private val sampleCalendar = CalendarUiState(
    month = YearMonth.now(),
    transactions = listOf(
        sampleTransaction(1, "스타벅스", 5_600),
        sampleTransaction(2, "급여 입금", 2_400_000, TransactionType.INCOME),
        sampleTransaction(3, "네이버페이 충전", 10_000, hidden = true),
        sampleTransaction(4, "GS25 편의점", 8_200),
    ),
    expense = 13_800L,       // 노출 리스트 기준 (무시 제외)
    income = 2_400_000L,
    minMonth = YearMonth.now().minusMonths(6),
    isLoading = false,
)

private val sampleAccounts = AccountUiState(
    accounts = listOf(
        Account(id = 1, packageName = "", bankName = "국민은행", accountPattern = "94160277777318", enabled = true),
        Account(id = 2, packageName = "", bankName = "카카오뱅크", accountPattern = "333311112222", enabled = false),
    ),
)

private val sampleIgnored = IgnoredAccountUiState(
    accounts = listOf(
        IgnoredAccount(id = 1, packageName = "com.kbstar.kbbank", sourceName = "국민은행", accountPattern = "94160277777318"),
        IgnoredAccount(id = 2, packageName = "com.Slack", sourceName = "Slack", accountPattern = ""),
    ),
    isLoading = false,
)

// ── 화면 프리뷰 ──────────────────────────────────────
@Preview(name = "1. 스플래시", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun SplashPreview() = AllowanceManagerTheme { SplashScreen() }

@Preview(name = "2. 인트로", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun IntroPreview() = AllowanceManagerTheme { IntroScreen() }

@Preview(name = "3. 온보딩·권한", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun OnboardingPermissionPreview() = AllowanceManagerTheme {
    OnboardingPermissionScreen(postGranted = true, listenerGranted = false, onAllow = {})
}

@Preview(name = "4. 온보딩·정보입력", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun OnboardingInfoPreview() = AllowanceManagerTheme {
    OnboardingInfoScreen(uiState = OnboardingUiState(payday = 25, paydayInput = "25", budgetInput = "500000"))
}

@Preview(name = "4-1. 온보딩·알림설정", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun OnboardingAlertPreview() = AllowanceManagerTheme {
    OnboardingAlertScreen(uiState = OnboardingUiState())
}

@Preview(name = "5. 홈", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun HomePreview() = AllowanceManagerTheme { HomeScreen(uiState = sampleHome) }

@Preview(name = "6. 월별", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun CalendarPreview() = AllowanceManagerTheme { CalendarScreen(uiState = sampleCalendar) }

@Preview(name = "7. 통계", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun StatsPreview() = AllowanceManagerTheme { StatsScreen(uiState = sampleStats) }

@Preview(name = "8. 설정", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun SettingPreview() = AllowanceManagerTheme {
    SettingScreen(
        uiState = SettingUiState(
            budget = 2_000_000L,
            payday = 25,
            statusBarEnabled = true,
            // "이번 달 월급일 조정" 행이 값 없이 비어 보이지 않게 실지급일 샘플을 준다
            paydayInfo = PaydayInfo(
                month = YearMonth.of(2026, 8),
                rule = 25,
                overrideDay = null,
                actual = LocalDate.of(2026, 8, 25),
                holidays = Holidays.EMPTY,
            ),
        ),
        versionName = "1.0.0",
        versionCode = 3,
    )
}

@Preview(name = "9. 계좌 관리", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun AccountPreview() = AllowanceManagerTheme { AccountSettingScreen(uiState = sampleAccounts) }

@Preview(name = "10. 무시 계좌 관리", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun IgnoredAccountPreview() = AllowanceManagerTheme {
    IgnoredAccountScreen(uiState = sampleIgnored)
}

// ── 상태별 검수 ──────────────────────────────────────
@Preview(name = "홈 · 예산 초과", group = "상태별", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun HomeOverPreview() = AllowanceManagerTheme {
    HomeScreen(uiState = sampleHome.copy(spent = 2_300_000, remaining = -300_000, ratio = 1.15f, isOver = true))
}

@Preview(name = "홈 · 빈 내역", group = "상태별", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun HomeEmptyPreview() = AllowanceManagerTheme {
    HomeScreen(uiState = sampleHome.copy(transactions = emptyList()))
}

@Preview(name = "계좌 · 없음", group = "상태별", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun AccountEmptyPreview() = AllowanceManagerTheme {
    AccountSettingScreen(uiState = AccountUiState(accounts = emptyList()))
}

@Preview(name = "월별 · 검색·필터", group = "상태별", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun CalendarSearchPreview() = AllowanceManagerTheme {
    CalendarScreen(uiState = sampleCalendar.copy(searchActive = true, query = "스타"))
}

@Preview(name = "월별 · 빈 내역", group = "상태별", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun CalendarEmptyPreview() = AllowanceManagerTheme {
    CalendarScreen(uiState = sampleCalendar.copy(transactions = emptyList(), expense = 0L, income = 0L))
}
