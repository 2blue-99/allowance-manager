package com.allowance.manager.service

import com.allowance.manager.core.domain.model.TransactionType
import timber.log.Timber

object NotificationParser {

    // 결제 알림을 보내는 금융 앱 패키지명 → 은행 이름 매핑
    private val PACKAGE_TO_BANK = mapOf(
        "com.shinhan.sbanking"                        to "신한은행",
        "com.kbstar.kbbank"                           to "KB국민은행",
        "com.kbcard.kbcardclient"                     to "KB국민카드",
        "com.samsung.android.spay"                    to "삼성페이",
        "viva.republica.toss"                         to "토스",
        "com.kakaopay.app"                            to "카카오페이",
        "com.nhn.android.naverapp"                    to "네이버페이",
        "nh.smart.nhallonebank"                       to "농협은행",
        "com.hanabank.ebk.channel.android.hananbank" to "하나은행",
        "com.wooribank.smart.wscd"                    to "우리은행",
        "com.ibk.spbs"                                to "IBK기업은행",
        "com.shinhancard.smartcalculator"             to "신한카드",
        "com.hyundaicard.appcard"                     to "현대카드",
        "com.lottemembers.android"                    to "롯데카드",
        "kr.co.citibank.citimobile"                   to "씨티은행",
    )

    private fun toBankName(packageName: String): String =
        PACKAGE_TO_BANK[packageName] ?: "미등록"

    // "입금 100,000" / "입금100,000" / "출금 100,000" / "출금100,000" 패턴
    private val TRANSACTION_REGEX = Regex("""(입금|출금)\s*([\d,]+)""")

    // "잔액225,023" 패턴
    private val BALANCE_REGEX = Regex("""잔액\s*([\d,]+)""")

    data class ParseResult(
        val type: TransactionType,
        val amount: Long,
        val totalAmount: Long,
        val bankName: String,
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

//        if (packageName !in PACKAGE_TO_BANK.keys) {
//            Timber.e("No Define Package Name Error")
//            return null
//        }

        val content = "${title.orEmpty()} ${text.orEmpty()}"
        if (content.isBlank()) return null

        val match = TRANSACTION_REGEX.find(content)

        val type = when (match?.groupValues[1]) {
            "입금" -> TransactionType.INCOME
            "출금" -> TransactionType.SPEND
            else -> null
        }

        val amount = match?.groupValues[2]
            ?.replace(",", "")
            ?.toLongOrNull()

        val remainingBalance = BALANCE_REGEX.find(content)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toLongOrNull()

        Timber.e("type : $type / amount : $amount / remainingBalance : $remainingBalance")

        // 모두 인식해야 저장하도록 구성
        if(type == null || amount == null || remainingBalance == null) {
            return null
        }

        return ParseResult(type = type, amount = amount, totalAmount = remainingBalance, bankName = toBankName(packageName))
    }
}
