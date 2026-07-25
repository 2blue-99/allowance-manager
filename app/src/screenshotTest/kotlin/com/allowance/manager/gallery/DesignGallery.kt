package com.allowance.manager.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.allowance.manager.core.designsystem.theme.AllowanceManagerTheme
import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.feature.account.AccountSettingScreen
import com.allowance.manager.feature.account.AccountUiState
import com.allowance.manager.feature.home.HomeScreen
import com.allowance.manager.feature.home.HomeUiState
import com.allowance.manager.feature.intro.IntroScreen
import com.allowance.manager.feature.onboarding.OnboardingInfoScreen
import com.allowance.manager.feature.onboarding.OnboardingPermissionScreen
import com.allowance.manager.feature.onboarding.OnboardingUiState
import com.allowance.manager.feature.setting.SettingScreen
import com.allowance.manager.feature.setting.SettingUiState
import com.allowance.manager.feature.splash.SplashScreen
import com.allowance.manager.feature.stats.MonthBar
import com.allowance.manager.feature.stats.StatsScreen
import com.allowance.manager.feature.stats.StatsUiState

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
    ignored: Boolean = false,
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
    isIgnored = ignored,
    createdAt = 1_753_000_000_000L,
)

private val sampleHome = HomeUiState(
    budget = 2_000_000L,
    spent = 760_000L,
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
        sampleTransaction(3, "네이버페이 충전", 10_000, ignored = true),
        sampleTransaction(4, "미등록 결제", 3_200, accountId = null),
    ),
    showMainOnly = false,
    canFilter = true,
    isLoading = false,
)

private val sampleStats = StatsUiState(
    currentMonthTotal = 760_000L,
    prevMonthDiff = 40_000L,
    bars = listOf(
        MonthBar("2월", 720_000, false),
        MonthBar("3월", 560_000, false),
        MonthBar("4월", 880_000, false),
        MonthBar("5월", 500_000, false),
        MonthBar("6월", 1_040_000, false),
        MonthBar("7월", 760_000, true),
    ),
    isLoading = false,
)

private val sampleAccounts = AccountUiState(
    accounts = listOf(
        Account(id = 1, packageName = "", bankName = "국민은행", accountPattern = "94160277777318", enabled = true),
        Account(id = 2, packageName = "", bankName = "카카오뱅크", accountPattern = "333311112222", enabled = false),
    ),
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
    OnboardingInfoScreen(uiState = OnboardingUiState(payday = 25, budgetInput = "500000"))
}

@Preview(name = "5. 홈", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun HomePreview() = AllowanceManagerTheme { HomeScreen(uiState = sampleHome) }

@Preview(name = "6. 통계", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun StatsPreview() = AllowanceManagerTheme { StatsScreen(uiState = sampleStats) }

@Preview(name = "7. 설정", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun SettingPreview() = AllowanceManagerTheme {
    SettingScreen(uiState = SettingUiState(budget = 2_000_000L, payday = 25, statusBarEnabled = true), versionName = "1.0.0")
}

@Preview(name = "8. 계좌 관리", group = GROUP, showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun AccountPreview() = AllowanceManagerTheme { AccountSettingScreen(uiState = sampleAccounts) }

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
