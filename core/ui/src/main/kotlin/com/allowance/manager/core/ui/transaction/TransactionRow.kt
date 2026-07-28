package com.allowance.manager.core.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType

/**
 * 내역 리스트 한 줄. 아이콘 + 사용처/시간(or 메모) + 부호 금액.
 * 무시 항목은 회색 카드 + 취소선, 미등록(비메인·비수동)은 흐림 + '미등록' 태그.
 */
@Composable
fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val ignored = tx.isIgnored
    // 수동 입력은 계좌가 없어도 정식 내역 → 미등록/흐림 대상 아님
    val dim = ignored || (!tx.isMain && !tx.isManual)
    val isIncome = tx.type == TransactionType.INCOME || tx.amount < 0

    AmCard(
        modifier = Modifier.fillMaxWidth(),
        // 무시 항목은 카드 자체를 회색으로 → 합계에서 빠졌음을 확실히 표현
        color = if (ignored) AmColors.IgnoredBg else AmColors.CardBg,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (dim) AmColors.Divider else AmColors.EmeraldBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(tx.category?.emoji ?: if (isIncome) "💰" else "💳", fontSize = 15.sp)
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            tx.sourceName,
                            style = AmType.label,
                            color = if (dim) AmColors.TextSecondary else AmColors.TextPrimary,
                            textDecoration = if (ignored) TextDecoration.LineThrough else TextDecoration.None,
                        )
                        if (ignored) {
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.CardBg).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("숨김", style = AmType.tag, color = AmColors.TextSecondary)
                            }
                        } else if (!tx.isMain && !tx.isManual) {
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.Divider).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("미등록", style = AmType.tag, color = AmColors.TextSecondary)
                            }
                        }
                    }
                    val subtitle = tx.memo?.takeIf { it.isNotBlank() } ?: formatTime(tx.createdAt)
                    Text(subtitle, style = AmType.tiny, color = AmColors.TextTertiary)
                }
            }
            Text(
                text = signedAmount(tx),
                style = AmType.labelStrong,
                color = amountColor(tx, dim),
                textDecoration = if (ignored) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
    }
}
