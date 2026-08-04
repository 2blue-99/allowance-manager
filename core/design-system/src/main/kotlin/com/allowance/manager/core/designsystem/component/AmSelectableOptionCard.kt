package com.allowance.manager.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing

/**
 * 선택형 옵션 카드. 아이콘(선택) + 제목 + 보조설명 + 우측 선택 체크로 구성된 한 줄 카드.
 * 선택 시 에메랄드 테두리·연한 배경·체크로 강조된다.
 * (설정 유형 선택 다이얼로그 등 "여러 옵션 중 하나 고르기"에 공통 사용)
 *
 * @param leading 좌측 아이콘 슬롯. 지정 시 둥근 컨테이너 안에 배치된다. (예: 이모지 Text)
 */
@Composable
fun AmSelectableOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val spec = tween<Color>(durationMillis = 150)
    val borderColor by animateColorAsState(
        if (selected) AmColors.Emerald else AmColors.BarTrack, spec, label = "optBorder",
    )
    val bg by animateColorAsState(
        if (selected) AmColors.EmeraldBg else AmColors.CardBg, spec, label = "optBg",
    )
    val titleColor by animateColorAsState(
        if (selected) AmColors.Emerald else AmColors.TextPrimary, spec, label = "optTitle",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AmShape.card)
            .border(1.5.dp, borderColor, AmShape.card)
            .background(bg)
            .amRippleClickable(onClick = onClick)
            .padding(horizontal = AmSpacing.lg, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(AmShape.cardSmall)
                    .background(if (selected) AmColors.CardBg else AmColors.NeutralBtnBg),
                contentAlignment = Alignment.Center,
            ) { leading() }
            Spacer(Modifier.width(AmSpacing.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = titleColor)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 13.sp, color = AmColors.TextSecondary)
        }
        SelectedIndicator(selected)
    }
}

// 우측 선택 표시 — 선택 시 에메랄드 채움 + 흰 체크, 미선택 시 빈 테두리 원.
@Composable
private fun SelectedIndicator(selected: Boolean) {
    if (selected) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AmColors.Emerald),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    } else {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(1.5.dp, AmColors.BarTrack, CircleShape),
        )
    }
}
