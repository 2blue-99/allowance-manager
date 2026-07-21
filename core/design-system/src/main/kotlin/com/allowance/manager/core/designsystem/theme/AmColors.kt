package com.allowance.manager.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * 앱 공통 팔레트 — 화면별 하드코딩 대신 여기 한 곳에서 관리.
 * (MaterialTheme colorScheme 밖에서 직접 쓰는 커스텀 색)
 */
object AmColors {
    // 브랜드
    val Navy = Color(0xFF0D1B2A)        // 히어로 배경·주요 텍스트
    val Emerald = Color(0xFF10B981)     // 액센트
    val EmeraldBg = Color(0xFFECFDF5)   // 액센트 연한 배경
    val Red = Color(0xFFEF4444)         // 지출·경고
    val RedBg = Color(0xFFFDECEC)

    // 서피스
    val ScreenBg = Color(0xFFF0F2F6)    // 화면 배경(라이트 그레이)
    val CardBg = Color(0xFFFFFFFF)      // 카드
    val IgnoredBg = Color(0xFFE2E6EC)   // 무시 처리된 항목 카드
    val ChipBg = Color(0xFFECECEC)      // 옵션 칩·플레이스홀더
    val Divider = Color(0xFFF4F6FA)
    val BarTrack = Color(0xFFE3E7EE)    // 차트 막대 트랙

    // 텍스트
    val TextPrimary = Navy
    val TextSecondary = Color(0xFF8A97AA)
    val TextTertiary = Color(0xFFA0AABB)

    // 다크 히어로 위 반투명 요소
    val HeroPillBg = Color(0x0FFFFFFF)
    val HeroPillLine = Color(0x14FFFFFF)
    val HeroRingTrack = Color(0x12FFFFFF)
    val HeroIconBg = Color(0x14FFFFFF)
}
