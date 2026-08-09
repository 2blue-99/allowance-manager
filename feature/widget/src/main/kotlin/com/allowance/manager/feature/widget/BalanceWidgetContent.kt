package com.allowance.manager.feature.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.domain.util.toCompactWon

@Composable
fun SimpleBalanceContent(budget: Long, spent: Long, remaining: Long) {
    val context = LocalContext.current
    val over = budget > 0 && remaining < 0
    val progress = when {
        budget <= 0 -> 0f
        over -> 1f
        else -> (spent.toFloat() / budget).coerceIn(0f, 1f)
    }
    val remainText = if (budget > 0) remaining.toCompactWon() else "예산 미설정"

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(AmColors.CardBg)
            .cornerRadius(20.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .clickable(actionStartActivity(context.launchAppIntent())),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemainChip()
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = remainText,
                style = TextStyle(
                    color = ColorProvider(if (over) AmColors.Red else AmColors.Navy),
                    fontSize = moneySp(remainText, 22),
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(modifier = GlanceModifier.height(7.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier.fillMaxWidth().height(9.dp).cornerRadius(6.dp),
            color = ColorProvider(if (over) AmColors.Red else AmColors.Emerald),
            backgroundColor = ColorProvider(AmColors.BarTrack),
        )
        Spacer(modifier = GlanceModifier.height(7.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "지출 ${spent.toCompactWon()}",
                style = TextStyle(color = ColorProvider(AmColors.TextSecondary), fontSize = 10.sp),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = if (budget > 0) "예산 ${budget.toCompactWon()}" else "예산 미설정",
                style = TextStyle(color = ColorProvider(AmColors.TextSecondary), fontSize = 10.sp),
            )
        }
    }
}

/** "남음" 회색 칩. */
@Composable
private fun RemainChip() {
    Text(
        text = "남음",
        modifier = GlanceModifier
            .background(AmColors.NeutralBtnBg)
            .cornerRadius(20.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = TextStyle(
            color = ColorProvider(AmColors.NeutralBtnText),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        ),
    )
}
