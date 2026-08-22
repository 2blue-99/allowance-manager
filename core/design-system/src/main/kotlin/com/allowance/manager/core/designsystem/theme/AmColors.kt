package com.allowance.manager.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * 앱 공통 팔레트 — 화면별 하드코딩 대신 여기 한 곳에서 관리.
 * (MaterialTheme colorScheme 밖에서 직접 쓰는 커스텀 색)
 */
object AmColors {
    // 브랜드
    val Navy = Color(0xFF0D1B2A)        // 주요 텍스트(TextPrimary)·아이콘
    val HeroBg = Color(0xFF283C58)      // 홈 예산 카드 배경(밝은 네이비)
    val Emerald = Color(0xFF10B981)     // 액센트
    val EmeraldDark = Color(0xFF0E9E6E) // 액센트 딥(후원 카드 그라데이션 등)
    val EmeraldBg = Color(0xFFECFDF5)   // 액센트 연한 배경
    val EmeraldMid = Color(0xFF5DBE9A)  // 연초록 배경 위 보조 텍스트
    val Red = Color(0xFFEF4444)         // 지출·경고
    val RedBg = Color(0xFFFDECEC)

    // 서피스
    val ScreenBg = Color(0xFFF0F2F6)    // 화면 배경(라이트 그레이)
    val CardBg = Color(0xFFFFFFFF)      // 카드
    val HiddenBg = Color(0xFFE2E6EC)    // 숨김 처리된 항목 카드
    val NeutralBtnBg = Color(0xFFEDEFF3)   // 보조(취소) 버튼 채움 배경
    val NeutralBtnText = Color(0xFF4E5968) // 보조 버튼 텍스트
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
    val HeroBarTrack = Color(0x1FFFFFFF)   // 다크 카드 위 진행바 트랙
    val HeroIconBg = Color(0x14FFFFFF)

    // ── 카테고리 색 (통계 도넛·랭킹, 향후 뱃지 공용) ──
    val CatFood = Color(0xFFFF8A5B)       // 식비
    val CatCafe = Color(0xFFFFC24B)       // 카페·간식
    val CatTransport = Color(0xFF4DA3FF)  // 교통
    val CatShopping = Color(0xFFFF6FB5)   // 쇼핑
    val CatFashion = Color(0xFFCE72C8)    // 패션·의류
    val CatBeauty = Color(0xFFDE6796)     // 뷰티·미용
    val CatLiving = Color(0xFF5AD1C0)     // 생활
    val CatHealth = Color(0xFFFF6B6B)     // 의료·건강
    val CatCulture = Color(0xFF9B8CFF)    // 문화·취미
    val CatEducation = Color(0xFF4EC38A)  // 교육
    val CatEvent = Color(0xFFC68A5E)      // 경조사·선물
    val CatHousing = Color(0xFF7C93B4)    // 주거·통신
    val CatIncome = Color(0xFF34D399)     // 수입
    val CatTransfer = Color(0xFFA9B4C4)   // 이체
    val CatEtc = Color(0xFFC7CDD6)        // 기타
    val CatNone = Color(0xFFD5DAE2)       // 미분류

    // ── 카테고리 칩 '선택' 채움색 — 흰 글씨 대비 확보용(밝은 도넛색은 톤 다운) ──
    val CatFoodOn = Color(0xFFFF8A5B)
    val CatCafeOn = Color(0xFFE8930C)
    val CatTransportOn = Color(0xFF3B93F5)
    val CatShoppingOn = Color(0xFFF45AA5)
    val CatFashionOn = Color(0xFFC05CBA)
    val CatBeautyOn = Color(0xFFC7466E)
    val CatLivingOn = Color(0xFF229E8D)
    val CatHealthOn = Color(0xFFF25555)
    val CatCultureOn = Color(0xFF8677F0)
    val CatEducationOn = Color(0xFF2FA971)
    val CatEventOn = Color(0xFFB67A4C)
    val CatHousingOn = Color(0xFF6A82A4)
    val CatIncomeOn = Color(0xFF10A472)
    val CatEtcOn = Color(0xFF98A2B0)
}
