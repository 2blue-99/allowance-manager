package com.allowance.manager.core.domain.model

data class Spending(
    val id: Long = 0,
    val amount: Long,
    val remainingBalance: Long,
    val timestamp: Long,
    val category: String = "",
    val memo: String = "",
)
