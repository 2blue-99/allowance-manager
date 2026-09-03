package com.allowance.manager.service

import com.allowance.manager.core.domain.model.TransactionType
import timber.log.Timber

/**
 * 금융 앱 결제/입출금 알림에서 금액·계좌·입출금 구분을 파싱.
 *
 * v3: 앱을 가리지 않고 받되(화이트리스트 없음), 입출금/결제 키워드 + "돈 형태" 금액(Layer 2)을
 * 갖춘 것만 거래로 인정.
 * - Layer 2: 금액이 `원` 단위(6,500원)이거나 잔액·마스킹 계좌번호를 동반할 때만 인정.
 *   맨숫자(예: 메신저 "[ABC] 1")는 탈락 → 노이즈 차단.
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
        val merchant: String?,     // 사용처(상대처). 못 뽑으면 null → 리스트는 sourceName로 폴백
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

        val balance = BALANCE_REGEX.find(content)?.groupValues?.get(1)?.toLongAmount()
        val account = ACCOUNT_REGEX.find(content)?.value

        // Layer 2: "돈 형태" 금액만 인정 (잔액/계좌번호 동반이 강한 신호). 맨숫자 노이즈는 여기서 탈락.
        val magnitude = extractMoneyAmount(content, hasStrongSignal = balance != null || account != null)
            ?: return null

        val type: TransactionType
        val amount: Long
        when {
            isRefund -> { type = TransactionType.EXPENSE; amount = -magnitude } // 취소·환불 = 마이너스 지출
            isIncome -> { type = TransactionType.INCOME; amount = magnitude }
            else -> { type = TransactionType.EXPENSE; amount = magnitude }
        }

        val sourceName = PACKAGE_TO_BANK[packageName] ?: (title ?: packageName)
        val merchant = extractMerchant(content, sourceName)

        // 사용처(merchant)는 개인정보 → 로그에 남기지 않는다 (추출 성공 여부만)
        Timber.d("parse: pkg=$packageName type=$type amount=$amount account=$account balance=$balance merchant=${merchant != null}")

        return ParseResult(
            type = type,
            amount = amount,
            balance = balance,
            extractedAccount = account,
            sourceName = sourceName,
            merchant = merchant,
            content = content,
        )
    }

    /**
     * Layer 2 — "돈 형태" 금액만 인정해 노이즈(메신저·메일 등)를 거른다.
     * - `원` 단위 금액(6,500원 / 1원)은 무조건 인정 (가장 강한 신호, 위치 무관)
     * - 원이 없으면 잔액·마스킹 계좌번호를 동반한 경우에만 키워드 뒤 숫자를 인정
     * - 그 외 맨숫자(예: 메신저 "[ABC] 승인 1")는 거래 아님 → null
     */
    private fun extractMoneyAmount(content: String, hasStrongSignal: Boolean): Long? {
        AMOUNT_WON_REGEX.find(content)?.groupValues?.get(1)?.toLongAmount()?.let { return it }
        if (hasStrongSignal) {
            AMOUNT_NEAR_KEYWORD_REGEX.find(content)?.groupValues?.get(1)?.toLongAmount()?.let { return it }
        }
        return null
    }

    private fun String.toLongAmount(): Long? = replace(",", "").toLongOrNull()

    // ─────────────────────── 사용처(상대처) 추출 ───────────────────────
    // 은행·카드 알림은 대부분 아래 골격을 따른다.
    //   [접두] [날짜] [시각] [마스킹계좌] ★상대처★ [거래구분][금액] [잔액]
    // → ① 마스킹 계좌 뒤 구간(대부분) ② 금액 뒤 구간(우리은행식) ③ 노이즈 소거 후 잔여 토큰 순으로 시도.
    // 애매하면 뽑지 않는다(null) → 리스트는 기존대로 sourceName(출처)로 폴백.

    /** 마스킹 계좌 토큰(추출 앵커). 별표 선행(*250284)·숫자 선행(941602-**-***318) 모두 인식 */
    private val MASKED_TOKEN_REGEX = Regex("""\*[\d*\-]{3,}|\d[\d\-]*\*[\d*\-]*""")

    /** 사용처 구간의 종료 경계 — 거래구분(채널 접두 포함)·잔액·금액 중 먼저 오는 것 */
    private val MERCHANT_END_REGEX = Regex(
        """[가-힣A-Za-z]{0,6}(?:입금|출금|결제|승인|이체|취소|환불|구매)|잔액|\d[\d,]*\s*원|\d{1,3}(?:,\d{3})+|\d{3,}"""
    )

    /** 후보에서 지울 노이즈 — [Web발신]·콜센터번호·마스킹 예금주(이*름님)·날짜·시각 */
    private val NOISE_TOKEN_REGEX = Regex(
        """\[[^\]]*\]|\d{2,4}-\d{3,4}(?:-\d{4})?|[가-힣][가-힣*]{0,9}님|\d{1,4}[/.]\d{1,2}(?:[/.]\d{1,2})?|\d{1,2}:\d{2}"""
    )

    /** 사용처에 남으면 안 되는 단어 — 거래구분·금액·잔액 + 결제수단/기관(체크카드·OO은행·페이…) */
    private val BANNED_MERCHANT_WORDS = listOf(
        "잔액", "누적", "승인", "출금", "입금", "결제", "이체", "취소", "환불", "구매", "한도", "원",
        "카드", "은행", "뱅크", "페이", "통장", "계좌", "할부", "일시불",
    )

    /**
     * 알림 본문에서 사용처(상대처)를 뽑는다. 입금이면 보낸 사람 이름이 들어온다.
     * 규칙이 모두 실패하거나 검증에 걸리면 null → 저장 시 merchant 미기록(기존 동작 유지).
     */
    private fun extractMerchant(content: String, sourceName: String): String? = runCatching {
        // ① 마스킹 계좌 뒤 ~ 경계 앞  ② 금액 뒤 ~ 경계 앞
        listOfNotNull(afterMaskedAccount(content), afterAmount(content))
            .map { it.untilBoundary().cleanMerchant() }
            .firstOrNull { it.isValidMerchant(sourceName) }
            ?: byElimination(content, sourceName)   // ③ 노이즈 소거 폴백
    }.getOrNull()

    /** ① 마스킹 계좌 뒤 구간 (KB·농협·현대카드 등 대부분) */
    private fun afterMaskedAccount(content: String): String? =
        MASKED_TOKEN_REGEX.find(content)?.let { content.substring(it.range.last + 1) }

    /** ② 금액 뒤 구간 (우리은행식: 출금 3,000원 서울특별시－현 잔액 …) */
    private fun afterAmount(content: String): String? {
        val match = AMOUNT_WON_REGEX.find(content) ?: AMOUNT_NEAR_KEYWORD_REGEX.find(content) ?: return null
        return content.substring(match.range.last + 1)
    }

    /** ③ 노이즈를 모두 지우고 남은 토큰이 정확히 하나일 때만 인정 (오추출 방지) */
    private fun byElimination(content: String, sourceName: String): String? {
        val stripped = content
            .replace(MASKED_TOKEN_REGEX, " ")
            .replace(NOISE_TOKEN_REGEX, " ")
            .replace(Regex("""[가-힣A-Za-z]{0,6}(?:입금|출금|결제|승인|이체|취소|환불|구매|사용|이용)"""), " ")
            .replace(Regex("""잔액\s*[\d,]*"""), " ")
            .replace(Regex("""[\d,]+\s*원?"""), " ")
            .replace(sourceName, " ")
        return stripped.split(Regex("""\s+"""))
            .map { it.cleanMerchant() }
            .filter { it.isValidMerchant(sourceName) }
            .singleOrNull()
    }

    /** 경계(거래구분·잔액·금액) 앞까지만 남긴다 */
    private fun String.untilBoundary(): String =
        substring(0, MERCHANT_END_REGEX.find(this)?.range?.first ?: length)

    private fun String.cleanMerchant(): String = NOISE_TOKEN_REGEX.replace(this, " ")
        .replace(Regex("""[\[\]()<>·,]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trim('-', '.', '/', ':', '*', '－', '·')
        .trim()

    /** 애매한 후보는 버린다: 길이 2~20 · 글자 1자 이상 · 금지어 미포함 · 출처명과 다름 */
    private fun String.isValidMerchant(sourceName: String): Boolean =
        length in 2..20 &&
            any { it.isLetter() } &&
            BANNED_MERCHANT_WORDS.none { contains(it) } &&
            !equals(sourceName.trim(), ignoreCase = true)
}
