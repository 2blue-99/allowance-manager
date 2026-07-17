package com.allowance.manager.core.domain.model

data class Spending(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val totalAmount: Long = 0L,
    val bankName: String = "",
    val timestamp: Long,
    val memo: String = "",
) {
    companion object {
        fun initSpending(totalAmount: Long): Spending =
            Spending(
                type = TransactionType.INCOME,
                amount = totalAmount,
                totalAmount = totalAmount,
                timestamp = System.currentTimeMillis(),
            )
    }
}

enum class TransactionType { INCOME, SPEND }
