package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.allowance.manager.core.designsystem.theme.AmColors

/**
 * 알림 시각 선택 다이얼로그 — Material3 [TimePicker]를 앱 공통 [AmDialog]로 감싸 브랜드 톤을 맞춘다.
 * 확인 시 선택된 시(0~23)·분(0~59)을 [onConfirm]으로 돌려준다. (온보딩·설정 공용)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    title: String = "알림 시각",
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false,
    )
    AmDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { onConfirm(state.hour, state.minute) },
        confirmText = "확인",
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = AmColors.ScreenBg,
                    clockDialSelectedContentColor = Color.White,
                    clockDialUnselectedContentColor = AmColors.TextPrimary,
                    selectorColor = AmColors.Emerald,
                    timeSelectorSelectedContainerColor = AmColors.EmeraldBg,
                    timeSelectorSelectedContentColor = AmColors.Emerald,
                    timeSelectorUnselectedContainerColor = AmColors.NeutralBtnBg,
                    timeSelectorUnselectedContentColor = AmColors.TextPrimary,
                    periodSelectorSelectedContainerColor = AmColors.EmeraldBg,
                    periodSelectorSelectedContentColor = AmColors.Emerald,
                    periodSelectorUnselectedContentColor = AmColors.TextSecondary,
                ),
            )
        }
    }
}
