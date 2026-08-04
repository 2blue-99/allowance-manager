package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.LocalAnalyticsHelper
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType

/**
 * 앱 공통 다이얼로그. 제목 + 자유 [content] 슬롯 + 확인/취소 버튼을 표준화.
 * Material AlertDialog 대신 이걸 써서 브랜드 색·모서리·타이포를 일관되게 유지한다.
 *
 * @param confirmEnabled false면 확인 버튼이 흐려지고 눌러도 동작하지 않는다.
 */
@Composable
fun AmDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "저장",
    dismissText: String = "취소",
    confirmEnabled: Boolean = true,
    confirmColor: Color = AmColors.Emerald,
    // 분석 태그(예: "delete","budget"). 지정 시 dialog_open/confirm/cancel 이벤트 자동 발사.
    analyticsTag: String? = null,
    analyticsParams: Map<String, Any?> = emptyMap(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val analytics = LocalAnalyticsHelper.current
    if (analyticsTag != null) {
        LaunchedEffect(analyticsTag) {
            analytics.logEvent(AmAnalytics.Event.DIALOG_OPEN, mapOf(AmAnalytics.Param.DIALOG to analyticsTag) + analyticsParams)
        }
    }
    fun cancel() {
        if (analyticsTag != null) analytics.logEvent(AmAnalytics.Event.DIALOG_CANCEL, mapOf(AmAnalytics.Param.DIALOG to analyticsTag))
        onDismiss()
    }
    Dialog(onDismissRequest = { cancel() }) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(AmShape.cardLarge)
                .background(AmColors.CardBg)
                .padding(AmSpacing.xl),
        ) {
            Text(title, style = AmType.dialogTitle, color = AmColors.TextPrimary)
            Spacer(Modifier.height(AmSpacing.xxl))
            content()
            Spacer(Modifier.height(AmSpacing.xxl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AmSpacing.md),
            ) {
                // dismissText가 비면 취소 버튼 숨김 (단일 버튼 다이얼로그 지원)
                if (dismissText.isNotBlank()) {
                    AmSecondaryButton(text = dismissText, onClick = { cancel() }, modifier = Modifier.weight(1f))
                }
                AmButton(
                    text = confirmText,
                    onClick = {
                        if (confirmEnabled) {
                            if (analyticsTag != null) {
                                analytics.logEvent(AmAnalytics.Event.DIALOG_CONFIRM, mapOf(AmAnalytics.Param.DIALOG to analyticsTag) + analyticsParams)
                            }
                            onConfirm()
                        }
                    },
                    enabled = confirmEnabled,
                    containerColor = confirmColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
