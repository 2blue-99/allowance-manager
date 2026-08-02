package com.allowance.manager.core.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.allowance.manager.core.designsystem.component.AmChip
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
