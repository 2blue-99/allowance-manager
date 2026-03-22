package com.allowance.manager.service

/**
 * 카드사·은행 앱의 결제 알림에서 사용 금액을 추출합니다.
 *
 * 지원 패턴:
 *   - "12,000원 사용" / "12,000원 승인" / "12,000원 결제" / "12,000원 출금"
 *   - 숫자 앞에 콤마 구분자 있거나 없는 경우 모두 처리
 */
object NotificationParser {

    // 결제 알림을 보내는 금융 앱 패키지명 목록
    private val FINANCIAL_PACKAGES = setOf(
        "com.shinhan.sbanking",          // 신한은행
        "com.kbstar.kbbank",             // KB국민은행
        "com.kbcard.kbcardclient",       // KB국민카드
        "com.samsung.android.spay",      // 삼성페이
        "viva.republica.toss",           // 토스
        "com.kakaopay.app",              // 카카오페이
        "com.nhn.android.naverapp",      // 네이버페이
        "nh.smart.nhallonebank",          // 농협은행
        "com.hanabank.ebk.channel.android.hananbank", // 하나은행
        "com.wooribank.smart.wscd",      // 우리은행
        "com.ibk.spbs",                  // IBK기업은행
        "com.shinhancard.smartcalculator", // 신한카드
        "com.hyundaicard.appcard",       // 현대카드
        "com.lottemembers.android",      // 롯데카드
        "kr.co.citibank.citimobile",     // 씨티은행
    )

    // "12,000원" 또는 "12000원" 패턴
    private val AMOUNT_REGEX = Regex("""([\d,]+)원""")

    // 결제 관련 키워드 (환불/취소는 제외)
    private val PAYMENT_KEYWORDS = listOf("사용", "승인", "결제", "출금", "이체")
    private val CANCEL_KEYWORDS = listOf("취소", "환불", "오류")

    data class ParseResult(
        val amount: Long,
        val packageName: String,
    )

    fun parse(packageName: String, title: String?, text: String?): ParseResult? {
        if (packageName !in FINANCIAL_PACKAGES) return null

        val content = "${title.orEmpty()} ${text.orEmpty()}"
        if (content.isBlank()) return null

        // 취소/환불 알림은 무시
        if (CANCEL_KEYWORDS.any { it in content }) return null

        // 결제 키워드가 없으면 무시
        if (PAYMENT_KEYWORDS.none { it in content }) return null

        val amount = AMOUNT_REGEX.find(content)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toLongOrNull()
            ?: return null

        if (amount <= 0) return null

        return ParseResult(amount = amount, packageName = packageName)
    }
}
