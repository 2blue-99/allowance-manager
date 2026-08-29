package com.allowance.manager.feature.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.allowance.manager.core.designsystem.theme.AmColors

/** 셀 하나 (라벨 + 값 + 값 색). */
internal data class CellData(val label: String, val value: String, val color: Color)

/**
 * 오늘 코치·가계부 공용 — 회색 디바이더로 나뉜 균등 셀 행. (카드 배경 + 앱 열기 포함)
 */
@Composable
internal fun CellsWidgetContent(cells: List<CellData>, valueBase: Int = 16) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(AmColors.CardBg)
            .cornerRadius(20.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .clickable(actionStartActivity(context.launchAppIntent())),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEachIndexed { index, cell ->
            if (index > 0) CellDivider()
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = cell.label,
                    style = TextStyle(
                        color = ColorProvider(AmColors.TextSecondary),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(5.dp))
                Text(
                    text = cell.value,
                    style = TextStyle(
                        color = ColorProvider(cell.color),
                        fontSize = moneySp(cell.value, valueBase),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CellDivider() {
    Box(
        modifier = GlanceModifier
            .width(1.dp)
            .height(34.dp)
            .background(Color(0xFFD5DAE2)),
    ) {}
}
