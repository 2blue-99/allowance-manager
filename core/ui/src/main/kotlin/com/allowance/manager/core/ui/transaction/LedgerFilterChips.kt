package com.allowance.manager.core.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.LedgerFilter
import com.allowance.manager.core.domain.model.LedgerFilterChip

/**
 * 내역 필터 칩 줄(메인 / 숨김 / 전체). 홈·월별 리스트 헤더에서 공용으로 쓴다.
 * 메인·숨김은 독립 토글이라 동시에 초록일 수 있고, 전체는 둘 다 꺼진 상태에서 선택 표시된다.
 */
@Composable
fun LedgerFilterChips(
    filter: LedgerFilter,
    onChip: (LedgerFilterChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LedgerFilterChip.entries.forEach { chip ->
            val selected = when (chip) {
                LedgerFilterChip.MAIN -> filter.showMain
                LedgerFilterChip.HIDDEN -> filter.showHidden
                LedgerFilterChip.ALL -> filter.isAll
            }
            AmChip(label = chip.label, selected = selected) { onChip(chip) }
        }
    }
}

/**
 * 내역 리스트 헤더(제목 + 필터 칩 + 선택적 우측 슬롯). 홈·월별 공용.
 * - 제목은 좌측, 칩은 우측 정렬
 * - [trailing]에 월별의 검색 토글 같은 부가 액션을 넣는다 (없으면 홈처럼 칩까지만)
 */
@Composable
fun LedgerListHeader(
    modifier: Modifier = Modifier,
    title: String = "입출금 내역",
    filter: LedgerFilter = LedgerFilter.Home,
    onFilterChip: (LedgerFilterChip) -> Unit = {},
    showChips: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = AmType.labelStrong, color = AmColors.TextPrimary)
        Spacer(Modifier.weight(1f))
        if (showChips) LedgerFilterChips(filter = filter, onChip = onFilterChip)
        trailing?.invoke()
    }
}
