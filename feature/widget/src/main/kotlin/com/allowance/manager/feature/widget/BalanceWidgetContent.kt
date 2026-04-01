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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
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

private val ColorPrimary   = Color(0xFF191F28)  // 거의 검정 (토스 스타일)
private val ColorSecondary = Color(0xFF8B95A1)  // 회색
private val ColorNormal    = Color.White
private val ColorFlash     = Color(0xFFEDF7EE)  // 연한 연두색

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

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(if (isFlashing) ColorFlash else ColorNormal)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // 상단: 라벨 + 새로고침 버튼
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "오늘 쓸 수 있어요",
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = ColorProvider(ColorSecondary),
                        fontSize = 12.sp,
                    ),
                )
                Image(
                    provider = ImageProvider(android.R.drawable.ic_popup_sync),
                    contentDescription = "새로고침",
                    modifier = GlanceModifier
                        .size(24.dp)
                        .background(Color.LightGray)
                        .clickable(onRefresh),
                )
            }

            // 메인: 하루 금액 (가장 크게)
            Text(
                text = "${formattedDaily}원",
                style = TextStyle(
                    color = ColorProvider(ColorPrimary),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            // 하단: 이번달 용돈 · 월급까지 N일
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "이번달 ${formattedMonth}원",
                    style = TextStyle(
                        color = ColorProvider(ColorSecondary),
                        fontSize = 11.sp,
                    ),
                )
                Text(
                    text = "  ·  ",
                    style = TextStyle(
                        color = ColorProvider(ColorSecondary),
                        fontSize = 11.sp,
                    ),
                )
                Text(
                    text = "월급까지 ${allowance.leftDays}일",
                    style = TextStyle(
                        color = ColorProvider(ColorSecondary),
                        fontSize = 11.sp,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 220, heightDp = 100)
@Composable
private fun BalanceWidgetPreview() {
    BalanceWidgetContent(allowance = Allowance(), onRefresh = {})
}
