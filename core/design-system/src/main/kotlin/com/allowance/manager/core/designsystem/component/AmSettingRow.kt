package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmType

/**
 * 설정/목록 행. 흰 카드 안에 [title](+선택 [subtitle]) + 우측 [trailing] 슬롯.
 * 설정의 값 표시/네비게이션/토글, 계좌 행 등에서 공통 사용.
 */
@Composable
fun AmSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    AmCard(
        modifier = modifier.fillMaxWidth(),
        shape = AmShape.cardSmall,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = AmType.bodyStrong, color = AmColors.TextPrimary)
                if (subtitle != null) {
                    Text(subtitle, style = AmType.caption, color = AmColors.TextSecondary)
                }
            }
            trailing?.invoke(this)
        }
    }
}

/** 우측 이동 표시 셰브런(›) */
@Composable
fun AmChevron() {
    Text("›", fontSize = 20.sp, color = AmColors.TextSecondary)
}

/**
 * 설정 섹션 그룹 — 상단 섹션 라벨(카드 사이 여백 정가운데) + 흰 카드 안에 여러 행.
 * 행([AmSettingItem]) 사이에는 구분선을 자동으로 넣는다. label이 null이면 라벨 없이 카드만.
 */
@Composable
fun AmSettingGroup(
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = AmType.labelStrong,
                color = AmColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 11.dp),
            )
        }
        AmCard(
            modifier = Modifier.fillMaxWidth(),
            shape = AmShape.cardSmall,
            contentPadding = PaddingValues(0.dp),
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(thickness = 1.dp, color = AmColors.Divider, modifier = Modifier.padding(start = 16.dp))
                    }
                    item()
                }
            }
        }
    }
}

/**
 * [AmSettingGroup] 안에 들어가는 행(자체 카드 없음). title(+subtitle) + 우측 trailing 슬롯.
 * 그룹 카드가 배경·모서리를 담당하므로 여기선 패딩과 클릭만 처리한다.
 */
@Composable
fun AmSettingItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .let {
            if (onClick != null) {
                it.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
            } else {
                it
            }
        }
        .padding(horizontal = 16.dp, vertical = 15.dp)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = AmType.bodyStrong, color = AmColors.TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = AmType.caption, color = AmColors.TextSecondary)
            }
        }
        trailing?.invoke(this)
    }
}
