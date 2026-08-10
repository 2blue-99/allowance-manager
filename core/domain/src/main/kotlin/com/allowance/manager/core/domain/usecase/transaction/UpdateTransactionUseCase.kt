package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 바텀시트 '저장' — 사용자가 수정한 항목(유형·금액·사용처·메모·분류)을 DB에 upsert.
 *
 * - 금액은 양수 크기(magnitude)로 받아, 부호는 원본 기준으로 결정한다.
 *   (수입=양수 / 취소·환불이던 지출은 음수 유지 / 일반 지출=양수)
 * - 메모는 공백만 있으면 null 로 정리한다.
 */
class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        id: Long,
        type: TransactionType,
        amount: Long,
        merchant: String,
        memo: String,
        category: TransactionCategory?,
    ) {
        val tx = transactionRepository.getById(id) ?: return
        val magnitude = kotlin.math.abs(amount)
        val signed = when {
            // 원래 취소·환불(마이너스 지출)이고 여전히 지출이면 부호 보존
            type == TransactionType.EXPENSE && tx.amount < 0 -> -magnitude
            else -> magnitude   // 수입·이체·일반 지출 = 양수
        }
        transactionRepository.update(
            tx.copy(
                type = type,
                amount = signed,
                // 출처(sourceName)는 읽기전용. 사용자가 편집하는 건 사용처(merchant)뿐.
                merchant = merchant.trim().ifBlank { null },
                memo = memo.trim().ifBlank { null },
                category = category,
            )
        )
    }
}
