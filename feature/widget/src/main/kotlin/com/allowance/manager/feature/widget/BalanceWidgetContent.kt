package com.allowance.manager.feature.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.allowance.manager.core.designsystem.theme.AmColors
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay

private val ColorPrimary = AmColors.Navy
private val ColorSecondary = AmColors.TextSecondary
private val ColorNormal = AmColors.CardBg
private val ColorFlash = AmColors.EmeraldBg
private val ColorOver = AmColors.Red

@Composable
fun BalanceWidgetContent(spent: Long, budget: Long, onRefresh: () -> Unit) {
    val formattedSpent = NumberFormat.getNumberInstance(Locale.KOREA).format(spent)
    val formattedBudget = NumberFormat.getNumberInstance(Locale.KOREA).format(budget)
    val percent = if (budget > 0) ((spent.toDouble() / budget) * 100).toInt() else 0
    val isOver = budget > 0 && spent > budget

    var isFlashing by remember { mutableStateOf(false) }
    LaunchedEffect(spent) {
        isFlashing = true
        delay(500)
        isFlashing = false
    }

    val context = LocalContext.current
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(if (isFlashing) ColorFlash else ColorNormal)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clickable(actionStartActivityIntent(launchIntent)),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "이번달 지출",
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(color = ColorProvider(ColorSecondary), fontSize = 12.sp),
                )
                Image(
                    provider = ImageProvider(android.R.drawable.ic_popup_sync),
                    contentDescription = "새로고침",
                    modifier = GlanceModifier.size(24.dp).clickable(onRefresh),
                )
            }

            Text(
                text = "${formattedSpent}원",
                style = TextStyle(
                    color = ColorProvider(if (isOver) ColorOver else ColorPrimary),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text = if (budget > 0) "예산 ${formattedBudget}원 · $percent%" else "예산 미설정",
                style = TextStyle(color = ColorProvider(ColorSecondary), fontSize = 11.sp),
            )
        }
    }
}

private fun actionStartActivityIntent(intent: Intent) =
    androidx.glance.appwidget.action.actionStartActivity(intent)

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 220, heightDp = 100)
@Composable
private fun BalanceWidgetPreview() {
    BalanceWidgetContent(spent = 186_500, budget = 300_000, onRefresh = {})
}
