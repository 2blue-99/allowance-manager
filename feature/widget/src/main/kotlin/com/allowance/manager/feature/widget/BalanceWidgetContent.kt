package com.allowance.manager.feature.widget

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
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.allowance.manager.core.domain.model.Allowance
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay

private val ColorFlash = Color(0xFFC8E6C9)   // 연두색
private val ColorNormal = Color.White

@Composable
fun BalanceWidgetContent(allowance: Allowance, onRefresh: () -> Unit) {
    val formattedMonth = NumberFormat.getNumberInstance(Locale.KOREA).format(allowance.monthAllowance)
    val formattedDaily = NumberFormat.getNumberInstance(Locale.KOREA).format(allowance.dailyAllowance)

    var isFlashing by remember { mutableStateOf(false) }

    LaunchedEffect(allowance) {
        isFlashing = true
        delay(500)
        isFlashing = false
    }

    val backgroundColor = if (isFlashing) ColorFlash else ColorNormal

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("월급까지")
                    Text("${allowance.leftDays}")
                }
                Spacer(modifier = GlanceModifier.width(4.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("이번달 용돈")
                    Text(formattedMonth)
                }
            }
            Row(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "하루 최대",
                        style = TextStyle(
                            color = ColorProvider(Color.Gray),
                            fontSize = 12.sp,
                        )
                    )
                    Text(
                        text = "${formattedDaily}원",
                        style = TextStyle(
                            color = ColorProvider(Color.Black),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Spacer(modifier = GlanceModifier.width(4.dp))
                Image(
                    provider = ImageProvider(android.R.drawable.ic_popup_sync),
                    contentDescription = "새로고침",
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(onRefresh),
                )
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 80)
@Composable
private fun BalanceWidgetPreview() {
    BalanceWidgetContent(allowance = Allowance(), onRefresh = {})
}
