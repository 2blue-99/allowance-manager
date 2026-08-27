package com.allowance.manager.feature.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmScreenHeader
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.usecase.alert.PaydayNoticeDecider
import com.allowance.manager.core.ui.month.MonthPickerDialog
import java.time.YearMonth

/**
 * 개발용 디버그 화면. debug 빌드에서 설정 맨 아래 진입점으로만 노출.
 * 인트로/온보딩 등 평소 진입하기 힘든 플로우로 바로 이동 + 홈 가이드 on/off.
 */
@Composable
fun DebugRoute(
    onBack: () -> Unit,
    onNavigateToIntro: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val homeGuideEnabled by viewModel.homeGuideEnabled.collectAsStateWithLifecycle()
    val budgetStatusText by viewModel.budgetStatusText.collectAsStateWithLifecycle()
    val paydayDebugText by viewModel.paydayDebugText.collectAsStateWithLifecycle()
    val remoteConfigText by viewModel.remoteConfigText.collectAsStateWithLifecycle()
    DebugScreen(
        onBack = onBack,
        onNavigateToIntro = onNavigateToIntro,
        onNavigateToOnboarding = onNavigateToOnboarding,
        homeGuideEnabled = homeGuideEnabled,
        onHomeGuideEnabledChange = viewModel::setHomeGuideEnabled,
        onSeedTestData = viewModel::seedTestData,
        onSeedCategories = viewModel::seedCategoriesForMonth,
        onApplyMonthSettings = viewModel::applyMonthSettings,
        budgetStatusText = budgetStatusText,
        onSpendTenPercent = viewModel::spendBudgetTenPercent,
        onResetSpending = viewModel::resetDebugSpending,
        onTriggerReminder = viewModel::triggerDailyReminderNow,
        paydayDebugText = paydayDebugText,
        onSetPaydayForTest = viewModel::setPaydayForTest,
        onClearPaydayForTest = viewModel::clearPaydayForTest,
        onTriggerPaydayAlert = viewModel::triggerPaydayAlertNow,
        onClearPaydaySentMark = viewModel::clearPaydayAlertSentMark,
        remoteConfigText = remoteConfigText,
        onFetchRemoteConfig = viewModel::fetchRemoteConfig,
    )
}

@Composable
fun DebugScreen(
    onBack: () -> Unit = {},
    onNavigateToIntro: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    homeGuideEnabled: Boolean = false,
    onHomeGuideEnabledChange: (Boolean) -> Unit = {},
    onSeedTestData: () -> Unit = {},
    onSeedCategories: (YearMonth) -> Unit = {},
    onApplyMonthSettings: (YearMonth, Int?, Long?, Long?, Long?) -> Unit = { _, _, _, _, _ -> },
    budgetStatusText: String = "",
    onSpendTenPercent: () -> Unit = {},
    onResetSpending: () -> Unit = {},
    onTriggerReminder: () -> Unit = {},
    paydayDebugText: String = "",
    onSetPaydayForTest: (Long) -> Unit = {},
    onClearPaydayForTest: () -> Unit = {},
    onTriggerPaydayAlert: (PaydayNoticeDecider.Notice?) -> Unit = {},
    onClearPaydaySentMark: () -> Unit = {},
    remoteConfigText: String = "",
    onFetchRemoteConfig: () -> Unit = {},
) {
    // 테스트 데이터 대상 달 (기본 이번 달) — 카테고리 시드·달별 세팅 공용 + 월 피커 노출 여부
    var seedMonth by remember { mutableStateOf(YearMonth.now()) }
    var showMonthPicker by remember { mutableStateOf(false) }
    // 달별 세팅 입력값
    var paydayInput by remember { mutableStateOf("") }
    var budgetInput by remember { mutableStateOf("") }
    var expenseInput by remember { mutableStateOf("") }
    var incomeInput by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmColors.ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(AmSpacing.xl),
    ) {
        AmScreenHeader(title = "디버그", onBack = onBack)
        Spacer(Modifier.height(AmSpacing.lg))
        Text("화면 이동", style = AmType.size12_bold, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.md))

        AmButton(text = "인트로 화면으로", onClick = onNavigateToIntro, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(AmSpacing.md))
        AmButton(text = "온보딩 화면으로", onClick = onNavigateToOnboarding, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(AmSpacing.xl))
        Text("가이드", style = AmType.size12_bold, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.md))
        // ON = 홈 재진입 시 가이드 노출 / OFF = 노출 안 함
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("홈 가이드 표시", style = AmType.size14_bold, color = AmColors.TextPrimary)
            Switch(checked = homeGuideEnabled, onCheckedChange = onHomeGuideEnabledChange)
        }

        Spacer(Modifier.height(AmSpacing.xl))
        Text("예산 소진 알림 (A안 테스트)", style = AmType.size12_bold, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.sm))
        // 현재 소진/남은 상태 — 소진·되돌리기 때마다 실시간 갱신
        Text(budgetStatusText, style = AmType.size14_bold, color = AmColors.TextPrimary)
        Spacer(Modifier.height(AmSpacing.md))
        // 누를 때마다 예산 10% 소진 → 남은 비율 10%p 하락 → 임계값 관통 시 알림
        AmButton(text = "예산 10% 소진하기", onClick = onSpendTenPercent, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(AmSpacing.sm))
        // 이 화면에서 넣은 디버그 지출을 전부 삭제 → 원래 잔액으로 복구
        AmButton(text = "원래대로 되돌리기", onClick = onResetSpending, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(AmSpacing.xl))
        Text("가계부 관리 알림 (B안 테스트)", style = AmType.size12_bold, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.md))
        // 리마인더 즉시 발화. "예산 10% 소진" 등으로 새 내역을 만든 뒤 눌러야 "안 본 내역" 판정을 통과해 알림이 뜬다.
        AmButton(text = "리마인더 지금 실행", onClick = onTriggerReminder, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(AmSpacing.xl))
        Text("Remote Config", style = AmType.size12_bold, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.sm))
        // 공휴일이 "내장 폴백"으로 나오면 원격 값을 못 받은 상태다
        Text(remoteConfigText, style = AmType.size14_bold, color = AmColors.TextPrimary)
        Spacer(Modifier.height(AmSpacing.md))
        AmButton(text = "지금 다시 받기 (fetch)", onClick = onFetchRemoteConfig, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(AmSpacing.xl))
        Text("월급일 알림 (C안 테스트)", style = AmType.size12_bold, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.sm))
        // 규칙일·조정값·실지급일·오늘 판정·마지막 발송 — 아래 버튼을 누를 때마다 갱신
        Text(paydayDebugText, style = AmType.size14_bold, color = AmColors.TextPrimary)
        Spacer(Modifier.height(AmSpacing.md))
        // 지급일을 오늘/내일로 지정하면 실제 경로(판정 포함)로 알림을 확인할 수 있다
        AmButton(
            text = "지급일을 내일로 지정 (전날 알림용)",
            onClick = { onSetPaydayForTest(1L) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AmSpacing.sm))
        AmButton(
            text = "지급일을 오늘로 지정 (당일 알림용)",
            onClick = { onSetPaydayForTest(0L) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AmSpacing.sm))
        AmButton(text = "지급일 지정 해제", onClick = onClearPaydayForTest, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(AmSpacing.md))
        // 실제 경로 — 오늘이 지급일 전날/당일이 아니면 아무것도 안 뜬다
        AmButton(
            text = "월급일 알림 지금 실행 (실제 판정)",
            onClick = { onTriggerPaydayAlert(null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AmSpacing.sm))
        // 중복 방지 표식 때문에 같은 날 두 번째부터 안 뜨므로, 다시 테스트할 땐 이걸 누른다
        AmButton(text = "발송 기록 지우기", onClick = onClearPaydaySentMark, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(AmSpacing.md))
        // 문구만 확인 — 판정·중복검사를 건너뛴다
        AmButton(
            text = "전날 문구 강제 발송",
            onClick = { onTriggerPaydayAlert(PaydayNoticeDecider.Notice.TOMORROW) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AmSpacing.sm))
        AmButton(
            text = "당일 문구 강제 발송",
            onClick = { onTriggerPaydayAlert(PaydayNoticeDecider.Notice.TODAY) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(AmSpacing.xl))
        Text("테스트 데이터", style = AmType.size12_bold, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.md))
        // 과거 12개월(1~12개월 전) 각 달 수동 지출 1건씩 삽입 → 월별·통계 이동 화살표 활성화 확인용
        AmButton(text = "과거 12개월 테스트 내역 넣기", onClick = onSeedTestData, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(AmSpacing.md))
        // 선택한 달에 카테고리별 1건씩 삽입 → 통계 카테고리 도넛 확인용
        AmButton(
            text = "대상 달: ${seedMonth.year}년 ${seedMonth.monthValue}월 (변경)",
            onClick = { showMonthPicker = true },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AmSpacing.sm))
        AmButton(
            text = "이 달에 카테고리별 1건씩 넣기",
            onClick = { onSeedCategories(seedMonth) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(AmSpacing.xl))
        Text("달별 세팅 (대상 달 기준)", style = AmType.size12_bold, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.md))
        // 월급일은 앱 전역 설정이라 달과 무관하게 전역 적용. 지출/수입은 그 달에 1건씩 추가.
        AmTextField(
            value = paydayInput,
            onValueChange = { v -> paydayInput = v.filter { it.isDigit() } },
            label = "월급일 (1~31, 0=말일 · 전역)",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AmSpacing.sm))
        AmTextField(
            value = budgetInput,
            onValueChange = { v -> budgetInput = v.filter { it.isDigit() } },
            label = "예산(용돈) 금액",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AmSpacing.sm))
        AmTextField(
            value = expenseInput,
            onValueChange = { v -> expenseInput = v.filter { it.isDigit() } },
            label = "지출 금액 (1건 추가)",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AmSpacing.sm))
        AmTextField(
            value = incomeInput,
            onValueChange = { v -> incomeInput = v.filter { it.isDigit() } },
            label = "수입 금액 (1건 추가)",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AmSpacing.md))
        AmButton(
            text = "대상 달에 적용",
            onClick = {
                onApplyMonthSettings(
                    seedMonth,
                    paydayInput.toIntOrNull()?.coerceIn(0, 31),
                    budgetInput.toLongOrNull(),
                    expenseInput.toLongOrNull(),
                    incomeInput.toLongOrNull(),
                )
                paydayInput = ""; budgetInput = ""; expenseInput = ""; incomeInput = ""
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            current = seedMonth,
            minMonth = null,
            maxMonth = YearMonth.now(),
            dataMonths = emptySet(),   // 디버그는 빈 달도 선택 가능해야 함 → 범위 방식 폴백
            onSelect = { seedMonth = it; showMonthPicker = false },
            onDismiss = { showMonthPicker = false },
        )
    }
}
