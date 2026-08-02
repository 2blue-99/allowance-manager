package com.allowance.manager.core.domain.model

/**
 * 사용자가 등록한 메인 계좌.
 * - accountPattern(마스킹 계좌번호)이 있으면: 알림 계좌번호와 자리별 대조로 인식 (은행)
 * - accountPattern이 비어 있으면(카드 등 계좌번호 없는 출처): 출처(앱 packageName)로 인식
 */
data class Account(
    val id: Long = 0,
    val packageName: String,
    val bankName: String,
    val accountPattern: String,
    val enabled: Boolean = true,
) {
    /** 계좌번호 기반 계좌인지(있으면 번호 매칭, 없으면 출처 매칭) */
    val isNumberBased: Boolean get() = accountPattern.isNotBlank()

    /**
     * 이 계좌가 해당 거래를 포함하는지 판정. (번호 AND 출처가 아니라, 계좌 종류별 매칭)
     * - 번호 계좌: 거래의 계좌번호가 자리별로 일치하면 매칭
     * - 출처 계좌: 거래에 **계좌번호가 없고** 같은 앱(packageName)이면 매칭
     *   (번호 있는 거래는 번호 계좌로만 매칭 → 같은 앱 다른 계좌 오집계 방지)
     */
    fun matchesTransaction(extractedAccount: String?, packageName: String): Boolean =
        if (isNumberBased) {
            MaskedAccount.matches(extractedAccount, accountPattern)
        } else {
            extractedAccount.isNullOrBlank() &&
                this.packageName.isNotBlank() && this.packageName == packageName
        }
}
