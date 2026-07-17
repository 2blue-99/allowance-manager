package com.allowance.manager.core.domain.model

/**
 * 사용자가 등록한 메인 계좌.
 * accountPattern(마스킹 계좌번호)이 알림 텍스트에 포함되면 해당 계좌 거래로 인식.
 */
data class Account(
    val id: Long = 0,
    val packageName: String,
    val bankName: String,
    val accountPattern: String,
    val enabled: Boolean = true,
)
