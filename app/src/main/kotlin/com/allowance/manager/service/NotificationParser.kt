package com.allowance.manager.service

import com.allowance.manager.core.domain.model.TransactionType
import timber.log.Timber

/**
 * 금융 앱 결제/입출금 알림에서 금액·계좌·입출금 구분을 파싱.
 *
 * v3: 모든 숫자 알림을 받되, 입출금/결제 키워드가 있는 건만 거래로 인정(노이즈 1차 필터).
 * 취소·환불 키워드는 마이너스(-) 지출로 처리.
 * 파싱 규칙은 우선 하드코딩, 이후 Remote Config로 확장 예정.
 */
object NotificationParser {

    private val PACKAGE_TO_BANK = mapOf(
        "com.shinhan.sbanking" to "신한은행",
        "com.kbstar.kbbank" to "KB국민은행",
        "com.kbcard.kbcardclient" to "KB국민카드",
        "com.samsung.android.spay" to "삼성페이",
        "viva.republica.toss" to "토스",
        "com.kakaopay.app" to "카카오페이",
        "com.nhn.android.naverapp" to "네이버페이",
        "nh.smart.nhallonebank" to "농협은행",
        "com.hanabank.ebk.channel.android.hananbank" to "하나은행",
        "com.wooribank.smart.wscd" to "우리은행",
        "com.ibk.spbs" to "IBK기업은행",
        "com.shinhancard.smartcalculator" to "신한카드",
        "com.hyundaicard.appcard" to "현대카드",
        "com.lottemembers.android" to "롯데카드",
        "kr.co.citibank.citimobile" to "씨티은행",
    )

    private val REFUND_KEYWORDS = listOf("취소", "환불", "반품")
    private val INCOME_KEYWORDS = listOf("입금", "이체입금", "급여")
    private val EXPENSE_KEYWORDS = listOf("출금", "결제", "사용", "이용", "구매", "이체출금", "승인")

    // "1,000원" / "1원" / "1,000 원" (한 자리 금액도 인식)
    private val AMOUNT_WON_REGEX = Regex("""(\d[\d,]*)\s*원""")
    // 키워드 뒤 금액: "입금 1,000" / "결제1,000" / "입금 1"
    private val AMOUNT_NEAR_KEYWORD_REGEX =
        Regex("""(?:입금|출금|결제|사용|이용|구매|승인|취소|환불)\s*(\d[\d,]*)""")
    // "잔액225,023"
    private val BALANCE_REGEX = Regex("""잔액\s*([\d,]+)""")
    // 마스킹 계좌번호: 숫자로 시작, * 를 포함하는 토큰 (예: 941602-**-***318, 9416***-61931**)
    private val ACCOUNT_REGEX = Regex("""\d[\d*\-]*\*[\d*\-]*""")

    data class ParseResult(
        val type: TransactionType,
        val amount: Long,          // 부호 포함 (취소/환불 음수)
        val balance: Long?,
        val extractedAccount: String?,
        val sourceName: String,
        val content: String,       // 계좌 매칭용 전체 텍스트
    )

    fun parse(packageName: String, title: String?, text: String?): ParseResult? {
        val content = "${title.orEmpty()} ${text.orEmpty()}".trim()
        if (content.isBlank()) return null

        val isRefund = REFUND_KEYWORDS.any { content.contains(it) }
        val isIncome = INCOME_KEYWORDS.any { content.contains(it) }
        val isExpense = EXPENSE_KEYWORDS.any { content.contains(it) }

        // 입출금/결제/취소 키워드가 하나도 없으면 거래 아님 → 노이즈로 무시
        if (!isRefund && !isIncome && !isExpense) return null

        val magnitude = extractAmount(content) ?: return null

        val type: TransactionType
        val amount: Long
        when {
            isRefund -> { type = TransactionType.EXPENSE; amount = -magnitude } // 취소·환불 = 마이너스 지출
            isIncome -> { type = TransactionType.INCOME; amount = magnitude }
            else -> { type = TransactionType.EXPENSE; amount = magnitude }
        }

        val balance = BALANCE_REGEX.find(content)?.groupValues?.get(1)?.toLongAmount()
        val account = ACCOUNT_REGEX.find(content)?.value

        Timber.d("parse: pkg=$packageName type=$type amount=$amount account=$account balance=$balance")

        return ParseResult(
            type = type,
            amount = amount,
            balance = balance,
            extractedAccount = account,
            sourceName = PACKAGE_TO_BANK[packageName] ?: (title ?: packageName),
            content = content,
        )
    }

    private fun extractAmount(content: String): Long? {
        AMOUNT_NEAR_KEYWORD_REGEX.find(content)?.groupValues?.get(1)?.toLongAmount()?.let { return it }
        AMOUNT_WON_REGEX.find(content)?.groupValues?.get(1)?.toLongAmount()?.let { return it }
        return null
    }

    private fun String.toLongAmount(): Long? = replace(",", "").toLongOrNull()
}
