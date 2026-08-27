package com.allowance.manager.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 앱 공통 모서리 토큰. 카드 12/16 혼용 등 불일치를 여기서 표준화.
 */
object AmShape {
    /** 작은 상태 라벨 칩 */
    val statusChip = RoundedCornerShape(6.dp)

    /** 선택형 칩 */
    val chip = RoundedCornerShape(9.dp)

    /** 설정 행 등 작은 카드 */
    val cardSmall = RoundedCornerShape(9.dp)

    /** 기본 카드 모서리 반경 (스와이프 시 한쪽 모서리 보간 등에 사용) */
    val cardRadius: Dp = 12.dp

    /** 기본 카드 */
    val card = RoundedCornerShape(cardRadius)

    /** 강조 카드(홈 예산 요약 등) */
    val cardLarge = RoundedCornerShape(18.dp)

    /** 바텀 시트 상단 */
    val sheetTop = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)

    /** 완전 둥근(원형 아이콘 버튼 등) */
    val pill = RoundedCornerShape(50)
}
