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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.TxScope

/**
 * 내역 리스트 한 줄. 아이콘 + 사용처/시간(or 메모) + 부호 금액.
 * 예산 반영 상태(scope)로 시각 구분: BUDGET 정상 / LEDGER_ONLY '예산 외' 태그 /
 * EXCLUDED 회색 카드 + '미등록'(자동)·'제외'(사용자) 태그. (취소선 없음)
 */
@Composable
fun TransactionRow(
    tx: Transaction,
    budgetName: String = "예산",
    shape: Shape = AmShape.card,
    onClick: () -> Unit,
) {
    val excluded = tx.scope == TxScope.EXCLUDED
    val ledgerOnly = tx.scope == TxScope.LEDGER_ONLY
    val isUnregistered = !tx.isMain && !tx.isManual   // 자동감지 · 계좌 미등록
    val dim = excluded                                 // 회색/흐림은 EXCLUDED만
    val isIncome = tx.type == TransactionType.INCOME || tx.amount < 0

    // 타이틀 = 사용처(고정) / 하단 = 시간, 메모 있으면 시간 뒤에 (한 줄, 넘치면 …)
    val memo = tx.memo?.takeIf { it.isNotBlank() }
    val time = formatListTimestamp(tx.createdAt)
    // 사용처(merchant)를 우선 표시, 없으면 출처(sourceName)로 대체
    val title = tx.merchant?.takeIf { it.isNotBlank() } ?: tx.sourceName
    val subtitle = if (memo != null) "$time · $memo" else time

    AmCard(
        modifier = Modifier.fillMaxWidth(),
        // EXCLUDED(미등록·제외)만 회색 카드 → 합계에서 빠졌음을 표현. LEDGER_ONLY는 정상 흰색.
        color = if (excluded) AmColors.HiddenBg else AmColors.CardBg,
        shape = shape,
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
                    Text(
                        tx.category?.emoji ?: when {
                            tx.type == TransactionType.TRANSFER -> "💸"
                            isIncome -> "💰"
                            else -> "💳"
                        },
                        fontSize = 15.sp,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            title,
                            style = AmType.size14_bold,
                            color = if (dim) AmColors.TextSecondary else AmColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            // 사용처가 길면 …로 줄이되 배지는 항상 보이게
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        // 상태 배지: 미등록(자동제외) / 제외(사용자) / 예산 외(가계부만)
                        if (excluded && isUnregistered) {
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.Divider).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("미등록", style = AmType.size8_bold, color = AmColors.TextSecondary)
                            }
                        } else if (excluded) {
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.CardBg).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("제외", style = AmType.size8_bold, color = AmColors.TextSecondary)
                            }
                        } else if (ledgerOnly) {
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(AmColors.ScreenBg).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("$budgetName 제외", style = AmType.size8_bold, color = AmColors.TextSecondary)
                            }
                        }
                    }
                    // 시간 + 메모: 한 줄, 넘치면 … 처리
                    Text(
                        subtitle,
                        style = AmType.size9_medium,
                        color = AmColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = signedAmount(tx),
                style = AmType.size12_bold,
                color = amountColor(tx, dim),
                maxLines = 1,
            )
        }
    }
}
