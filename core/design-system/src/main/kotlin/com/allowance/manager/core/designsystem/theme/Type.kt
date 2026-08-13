package com.allowance.manager.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.R

val pretendard = FontFamily(
    Font(R.font.pretendard_thin, FontWeight.Thin),           // 100
    Font(R.font.pretendard_light, FontWeight.Light),         // 300
    Font(R.font.pretendard_regular, FontWeight.Normal),      // 400
    Font(R.font.pretendard_medium, FontWeight.Medium),       // 500
    Font(R.font.pretendard_semi_bold, FontWeight.SemiBold),  // 600
    Font(R.font.pretendard_bold, FontWeight.Bold),           // 700
    Font(R.font.pretendard_extra_bold, FontWeight.ExtraBold),// 800
    Font(R.font.pretendard_black, FontWeight.Black),         // 900
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

/**
 * 앱 공통 폰트 토큰 — **크기 × 굵기** 조합만으로 구성한다. (의미 기반 이름 금지)
 *
 * 이름 규칙: `size{px}_{weight}`
 * - 굵기: `thin`(100) · `light`(300) · `medium`(500) · `bold`(700) · `black`(900)
 * - 화면에서 `fontSize`/`fontWeight`를 직접 쓰지 말고 `style = AmType.size14_medium` 처럼 쓴다.
 * - 색·자간은 별개 관심사 → `Text`의 `color` 등 파라미터로 지정.
 * - 필요한 조합이 없으면 아래에 한 줄 추가한다.
 */
object AmType {
    private fun t(size: Int, weight: FontWeight) =
        TextStyle(fontFamily = pretendard, fontSize = size.sp, fontWeight = weight)

    // black (900) — 완전볼드
    val size36_black = t(36, FontWeight.Black)
    val size22_black = t(22, FontWeight.Black)
    val size18_black = t(18, FontWeight.Black)
    val size17_black = t(17, FontWeight.Black)
    val size14_black = t(14, FontWeight.Black)
    val size13_black = t(13, FontWeight.Black)
    val size12_black = t(12, FontWeight.Black)

    // bold (700)
    val size14_bold = t(14, FontWeight.Bold)
    val size13_bold = t(13, FontWeight.Bold)
    val size12_bold = t(12, FontWeight.Bold)
    val size11_bold = t(11, FontWeight.Bold)
    val size10_bold = t(10, FontWeight.Bold)
    val size8_bold = t(8, FontWeight.Bold)

    // medium (500)
    val size14_medium = t(14, FontWeight.Medium)
    val size12_medium = t(12, FontWeight.Medium)
    val size11_medium = t(11, FontWeight.Medium)
    val size10_medium = t(10, FontWeight.Medium)
    val size9_medium = t(9, FontWeight.Medium)
}
