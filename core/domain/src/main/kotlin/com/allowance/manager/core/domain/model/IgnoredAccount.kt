package com.allowance.manager.core.domain.model

/**
 * 무시한 출처/계좌. 매칭되는 알림은 파싱 후 저장하지 않고 드롭한다. (숨김과 다름 — 아예 기록 안 함)
 * - accountPattern 있으면: 마스킹 계좌번호 자리별 대조 (그 계좌만)
 * - accountPattern 비면: 출처(packageName)로 대조 (카드·메신저 등, 그 앱 전체)
 *
 * 매칭 규칙은 [Account.matchesTransaction]과 동일하다.
 */
data class IgnoredAccount(
    val id: Long = 0,
    val packageName: String,
    val sourceName: String,
    val accountPattern: String,
    val createdAt: Long = 0,
) {
    /** 계좌번호 기반 무시인지(있으면 번호 매칭, 없으면 출처 매칭) */
    val isNumberBased: Boolean get() = accountPattern.isNotBlank()

    fun matches(extractedAccount: String?, packageName: String): Boolean =
        if (isNumberBased) {
            MaskedAccount.matches(extractedAccount, accountPattern)
        } else {
            extractedAccount.isNullOrBlank() &&
                this.packageName.isNotBlank() && this.packageName == packageName
        }
}
