package com.allowance.manager.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.ui.R

val pretendard = FontFamily(
    Font(R.font.pretendard_light, FontWeight.Light),
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semi_bold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extra_bold, FontWeight.ExtraBold),
)

val typography = Typography(
    displayLarge = TextStyle(fontFamily = pretendard),
    displayMedium = TextStyle(fontFamily = pretendard),
    displaySmall = TextStyle(fontFamily = pretendard),
    headlineLarge = TextStyle(fontFamily = pretendard),
    headlineMedium = TextStyle(fontFamily = pretendard),
    headlineSmall = TextStyle(fontFamily = pretendard),
    titleLarge = TextStyle(fontFamily = pretendard),
    titleMedium = TextStyle(fontFamily = pretendard),
    titleSmall = TextStyle(fontFamily = pretendard),
    bodyLarge = TextStyle(fontFamily = pretendard),
    bodyMedium = TextStyle(fontFamily = pretendard),
    bodySmall = TextStyle(fontFamily = pretendard),
    labelLarge = TextStyle(fontFamily = pretendard),
    labelMedium = TextStyle(fontFamily = pretendard),
    labelSmall = TextStyle(fontFamily = pretendard),
)

object AmStyle {
    val text32 = TextStyle(
        fontFamily = pretendard,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
    )
    val text28 = TextStyle(
        fontFamily = pretendard,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
    )
    val text24 = TextStyle(
        fontFamily = pretendard,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
    )
    val text20 = TextStyle(
        fontFamily = pretendard,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val text18 = TextStyle(
        fontFamily = pretendard,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val text16 = TextStyle(
        fontFamily = pretendard,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
    )
    val text14 = TextStyle(
        fontFamily = pretendard,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
    )
    val text12 = TextStyle(
        fontFamily = pretendard,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
    )
    val text10 = TextStyle(
        fontFamily = pretendard,
        fontSize = 10.sp,
        fontWeight = FontWeight.Light,
    )
}
