package com.allowance.manager.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.R

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

/**
 * 앱 공통 타이포그래피 토큰(의미 기반).
 * 화면에서 fontSize/fontWeight를 직접 쓰지 말고 `style = AmType.xxx` 로 사용한다.
 * (색은 별개 관심사 → Text의 color 파라미터로 지정)
 */
object AmType {
    private fun t(size: Int, weight: FontWeight, ls: androidx.compose.ui.unit.TextUnit = 0.sp) =
        TextStyle(fontFamily = pretendard, fontSize = size.sp, fontWeight = weight, letterSpacing = ls)

    // 금액·큰 숫자
    val amountHero = t(36, FontWeight.ExtraBold, (-1.2).sp)   // 홈 남은 용돈(메인 강조)
    val amountLarge = t(28, FontWeight.ExtraBold)             // 통계 총액

    // 제목
    val titleXl = t(30, FontWeight.ExtraBold)                 // 온보딩 대제목
    val titleLg = t(24, FontWeight.ExtraBold)                 // 인트로 제목
    val title = t(22, FontWeight.ExtraBold)                   // 화면 타이틀·섹션 질문
    val titleSm = t(20, FontWeight.ExtraBold)                 // 스플래시 앱명
    val header = t(18, FontWeight.ExtraBold)                  // 상단 헤더
    val dialogTitle = t(17, FontWeight.ExtraBold)

    // 본문
    val bodyLarge = t(16, FontWeight.Normal)
    val body = t(14, FontWeight.Normal)
    val bodyStrong = t(14, FontWeight.Bold)                   // 설정 행 제목, 계좌명

    // 값·강조
    val emphasis = t(15, FontWeight.ExtraBold)                // 시트 소스명·금액
    val valueStrong = t(13, FontWeight.ExtraBold)             // 홈/통계 강조 값
    val value = t(13, FontWeight.Bold)                        // 설정 값, 칩
    val bodySmall = t(13, FontWeight.Normal)                  // 스플래시 서브타이틀

    // 라벨·캡션
    val labelStrong = t(12, FontWeight.ExtraBold)             // 리스트 헤더, %·금액
    val label = t(12, FontWeight.Bold)                        // 거래명
    val labelSoft = t(12, FontWeight.SemiBold, 0.3.sp)        // 히어로 라벨
    val labelMuted = t(12, FontWeight.Normal)                 // 보조 라벨
    val captionStrong = t(11, FontWeight.Bold)               // 토글 라벨·상태 칩
    val caption = t(11, FontWeight.Normal)                    // 캡션·시간
    val microStrong = t(10, FontWeight.Bold)
    val micro = t(10, FontWeight.Normal)                      // 아주 작은 라벨
    val tinyStrong = t(9, FontWeight.Bold)
    val tiny = t(9, FontWeight.Normal)
    val tag = t(8, FontWeight.Bold)                           // 숨김·미등록 태그
}
