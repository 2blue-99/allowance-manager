package com.allowance.manager.service

import timber.log.Timber

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

    // "잔액225,023" 패턴 (원 단위 없이 숫자만 붙는 경우)
    private val BALANCE_REGEX = Regex("""잔액([\d,]+)""")

    // 결제 관련 유효 키워드 (환불/취소는 제외)
    private val PAYMENT_KEYWORDS = listOf("잔액")
//    private val PAYMENT_KEYWORDS = listOf("사용", "승인", "결제", "출금", "이체", "입금", "잔액")

    data class ParseResult(
        val amount: Long,
        val packageName: String,
    )

    /**
     * 14:48:29.055  E  packageName : com.kbstar.kbbank
     * 14:48:29.055  E  title : 입금 1,000원
     * 14:48:29.055  E  text : 이*름님 04/01 14:48 941602-**-***318 이푸름 FBS입금 1,000 잔액225,023
     */
    fun parse(packageName: String, title: String?, text: String?): ParseResult? {
        Timber.e("packageName : $packageName")
        Timber.e("title : $title")
        Timber.e("text : $text")

//        if (packageName !in FINANCIAL_PACKAGES) {
//            Timber.e("No Define Package Name Error")
//            return null
//        }

        val content = "${title.orEmpty()} ${text.orEmpty()}"
        if (content.isBlank()) {
            Timber.e("Empty Content Error")
            return null
        }

        // 잔액 키워드가 없으면 무시
        if (PAYMENT_KEYWORDS.none { it in content }) {
            Timber.e("Empty Keyword")
            return null
        }

        val amount = (BALANCE_REGEX.find(content) ?: AMOUNT_REGEX.find(content))
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toLongOrNull()
            ?: run {
                Timber.e("amount error")
                return null
            }

        if (amount <= 0) {
            Timber.e("amount <= 0")
            return null
        }

        return ParseResult(amount = amount, packageName = packageName)
    }
}
