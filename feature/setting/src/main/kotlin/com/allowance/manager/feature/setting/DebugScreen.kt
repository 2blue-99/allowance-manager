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
    val calendarSearchAutoEnabled by viewModel.calendarSearchAutoEnabled.collectAsStateWithLifecycle()
    DebugScreen(
        onBack = onBack,
        onNavigateToIntro = onNavigateToIntro,
        onNavigateToOnboarding = onNavigateToOnboarding,
        homeGuideEnabled = homeGuideEnabled,
        onHomeGuideEnabledChange = viewModel::setHomeGuideEnabled,
        calendarSearchAutoEnabled = calendarSearchAutoEnabled,
        onCalendarSearchAutoEnabledChange = viewModel::setCalendarSearchAutoEnabled,
        onSeedTestData = viewModel::seedTestData,
        onSeedCategories = viewModel::seedCategoriesForMonth,
        onApplyMonthSettings = viewModel::applyMonthSettings,
    )
}

@Composable
fun DebugScreen(
    onBack: () -> Unit = {},
    onNavigateToIntro: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    homeGuideEnabled: Boolean = false,
    onHomeGuideEnabledChange: (Boolean) -> Unit = {},
    calendarSearchAutoEnabled: Boolean = false,
    onCalendarSearchAutoEnabledChange: (Boolean) -> Unit = {},
    onSeedTestData: () -> Unit = {},
    onSeedCategories: (YearMonth) -> Unit = {},
    onApplyMonthSettings: (YearMonth, Int?, Long?, Long?, Long?) -> Unit = { _, _, _, _, _ -> },
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
        Spacer(Modifier.height(AmSpacing.md))
        // ON = 월별 재진입 시 검색·필터 자동 노출 / OFF = 노출 안 함 (최초 1회 노출 초기화용)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("월별 검색 자동 노출", style = AmType.size14_bold, color = AmColors.TextPrimary)
            Switch(checked = calendarSearchAutoEnabled, onCheckedChange = onCalendarSearchAutoEnabledChange)
        }

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
