package com.allowance.manager.core.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType

/**
 * 내역 리스트 한 줄. 아이콘 + 사용처/시간(or 메모) + 부호 금액.
 * 숨김 항목은 회색 카드 + 취소선, 미등록(비메인·비수동)은 흐림 + '미등록' 태그.
 */
@Composable
fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val hidden = tx.isHidden
    // 수동 입력은 계좌가 없어도 정식 내역 → 미등록/흐림 대상 아님
    val dim = hidden || (!tx.isMain && !tx.isManual)
    val isIncome = tx.type == TransactionType.INCOME || tx.amount < 0

    // 타이틀 = 사용처(고정) / 하단 = 시간, 메모 있으면 시간 뒤에 (한 줄, 넘치면 …)
    val memo = tx.memo?.takeIf { it.isNotBlank() }
    val time = formatListTimestamp(tx.createdAt)
    val title = tx.sourceName
    val subtitle = if (memo != null) "$time · $memo" else time

    AmCard(
        modifier = Modifier.fillMaxWidth(),
        // 숨김 항목은 카드 자체를 회색으로 → 합계에서 빠졌음을 확실히 표현
        color = if (hidden) AmColors.HiddenBg else AmColors.CardBg,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (dim) AmColors.Divider else AmColors.EmeraldBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(tx.category?.emoji ?: if (isIncome) "💰" else "💳", fontSize = 15.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            title,
                            style = AmType.label,
                            color = if (dim) AmColors.TextSecondary else AmColors.TextPrimary,
                            textDecoration = if (hidden) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            // 사용처가 길면 …로 줄이되 배지는 항상 보이게
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (hidden) {
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.CardBg).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("숨김", style = AmType.tag, color = AmColors.TextSecondary)
                            }
                        } else if (!tx.isMain && !tx.isManual) {
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.Divider).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("미등록", style = AmType.tag, color = AmColors.TextSecondary)
                            }
                        }
                    }
                    // 시간 + 메모: 한 줄, 넘치면 … 처리
                    Text(
                        subtitle,
                        style = AmType.tiny,
                        color = AmColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = signedAmount(tx),
                style = AmType.labelStrong,
                color = amountColor(tx, dim),
                textDecoration = if (hidden) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
            )
        }
    }
}
