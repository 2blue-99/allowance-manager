package com.allowance.manager.core.domain.model

data class Allowance(
    val monthAllowance: Long = 0L,
    val dailyAllowance: Long = 0L,
    val leftDays: Long = 0L
)
