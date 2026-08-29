package com.allowance.manager.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmChevron
import com.allowance.manager.core.designsystem.component.AmSelectableOptionCard
import com.allowance.manager.core.designsystem.component.AmTimePickerDialog
import com.allowance.manager.core.designsystem.component.AmToggle
import com.allowance.manager.core.designsystem.component.amRippleClickable
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.domain.model.AlertFrequency
import com.allowance.manager.core.domain.util.formatTimeOfDay

private val Accent = AmColors.Emerald
private val ScreenBg = AmColors.CardBg
private val TextPrimary = AmColors.TextPrimary
private val TextSecondary = AmColors.TextSecondary

/**
 * 온보딩 마지막 스텝 — 알림 설정. (「기본 정보를 입력해요」 화면과 동일한 타이포·간격 규격)
 * 한 화면에 예산 소진 알림 / 가계부 관리 알림 두 섹션. 각 섹션 헤더의 토글로 켜고,
 * 켜지면 아래 옵션(빈도 카드 / 알림 시각)이 펼쳐진다.
 * 다이얼로그 노출 같은 순수 UI 상태는 Screen이 보유한다.
 */
@Composable
fun OnboardingAlertScreen(
    uiState: OnboardingUiState,
    onBudgetAlertToggle: (Boolean) -> Unit = {},
    onBudgetAlertFrequencyChange: (AlertFrequency) -> Unit = {},
    onDailyReminderToggle: (Boolean) -> Unit = {},
    onReminderTimeChange: (Int, Int) -> Unit = { _, _ -> },
    onFinish: () -> Unit = {},
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(horizontal = 24.dp)
            .padding(top = 80.dp),
    ) {
        Text("관리에 도움을 줄 알림이에요", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("설정에서 언제든 바꿀 수 있어요.", fontSize = 16.sp, color = TextSecondary)
        Spacer(Modifier.height(36.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            // 예산 소진 알림
            AlertSection(
                title = "예산 소진 알림",
                subtitle = "예산이 줄어들 때 알려드려요.",
                checked = uiState.budgetAlertEnabled,
                onCheckedChange = onBudgetAlertToggle,
            ) {
                AlertFrequency.entries.forEach { freq ->
                    AmSelectableOptionCard(
                        title = freq.displayLabel,
                        subtitle = freq.summary,
                        selected = uiState.budgetAlertFrequency == freq,
                        onClick = { onBudgetAlertFrequencyChange(freq) },
                    )
                }
            }

            // 가계부 관리 알림
            AlertSection(
                title = "가계부 관리 알림",
                subtitle = "그날 내역이 있으면 확인 알림을 보내요.",
                checked = uiState.dailyReminderEnabled,
                onCheckedChange = onDailyReminderToggle,
            ) {
                TimeRow(
                    time = formatTimeOfDay(uiState.reminderHour, uiState.reminderMinute),
                    onClick = { showTimePicker = true },
                )
            }
        }

        Spacer(Modifier.height(AmSpacing.md))
        AmButton(text = "시작하기", onClick = onFinish, modifier = Modifier.fillMaxWidth())
    }

    if (showTimePicker) {
        AmTimePickerDialog(
            initialHour = uiState.reminderHour,
            initialMinute = uiState.reminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                onReminderTimeChange(hour, minute)
                showTimePicker = false
            },
        )
    }
}

// 섹션 — 제목(22sp ExtraBold, InfoScreen의 Section과 동일)+보조설명 + 우측 토글.
// 켜지면 아래 옵션이 펼쳐진다.
@Composable
private fun AlertSection(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 14.sp, color = TextSecondary)
            }
            AmToggle(checked = checked, onCheckedChange = onCheckedChange)
        }
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(top = AmSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

// 알림 시각 행 — 흰 배경 화면에서 구분되도록 연한 그레이 채움. (화면 고유 레이아웃)
@Composable
private fun TimeRow(time: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AmShape.card)
            .background(AmColors.ScreenBg)
            .amRippleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("알림 시각", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(time, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Accent)
        Spacer(Modifier.width(AmSpacing.xs))
        AmChevron()
    }
}
