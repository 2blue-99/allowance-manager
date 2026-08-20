package com.allowance.manager.core.domain.util

import kotlin.math.abs

/**
 * 앱 공통 금액 표기 규칙. (위젯·화면에서 좁은 폭에 금액을 넣을 때 사용)
 *
 * 경계 = 1,000,000원.
 * - 999,999원 이하(십만원대까지): 천단위 콤마 + "원"  →  113,500원
 * - 1,000,000원 이상(백만·천만원대): 만 단위(만원 반올림)  →  285만원 · 1,500만원
 * - 1억 이상: 억 + 만  →  1억 2,345만원 · 3억원
 * - 음수(예산 초과 등)는 동일 규칙에 "-" 부호  →  -24,000원 · -240만원
 */
fun Long.toCompactWon(): String {
    val sign = if (this < 0) "-" else ""
    val abs = abs(this)

    if (abs < 1_000_000L) return "$sign${abs.amountToComma()}원"

    // 만 단위 반올림 (5,000원에서 올림)
    val manTotal = (abs + 5_000L) / 10_000L
    val eok = manTotal / 10_000L      // 1억 = 10,000만
    val man = manTotal % 10_000L

    val body = buildString {
        if (eok > 0) append("${eok.amountToComma()}억")
        if (man > 0) {
            if (eok > 0) append(" ")
            append("${man.amountToComma()}만")
        }
        append("원")
    }
    return "$sign$body"
}

/**
 * 아주 좁은 폭(통계 막대 라벨 등)용 초압축 표기. **절삭** + **최상위 두 단위**까지만, 붙여서.
 *
 * - 0                       → "0"
 * - 1 ~ 999                 → "약1천"      (1천원 미만은 뭉뚱그림)
 * - 1,000 ~ 9,999           → "3천"        (천 단위 절삭)
 * - 10,000 ~ 9,999만        → "8만2천" · "12만8천" · "120만" · "1200만"  (만 블록 풀 + 그 아래 천 한 자리)
 * - 1억 이상                → "1억" · "1억3천만" · "1억5백만" · "12억"     (억 풀 + 그 아래 첫 단위 한 자리)
 *
 * [toCompactWon]과 달리 반올림·콤마·"원" 접미사·띄어쓰기가 없다.
 */
fun Long.toCompactShort(): String {
    if (this == 0L) return "0"
    val sign = if (this < 0) "-" else ""
    val abs = abs(this)

    val body = when {
        abs < 1_000L -> "약1천"
        abs < 10_000L -> "${abs / 1_000L}천"
        abs < 100_000_000L -> {
            val man = abs / 10_000L            // 1 ~ 9,999
            val thou = (abs % 10_000L) / 1_000L // 0 ~ 9
            if (thou > 0) "${man}만${thou}천" else "${man}만"
        }
        else -> {
            val eok = abs / 100_000_000L       // 1억 = 100,000,000
            val rem = abs % 100_000_000L       // 0 ~ 99,999,999
            val second = when {
                rem >= 10_000_000L -> "${rem / 10_000_000L}천만"
                rem >= 1_000_000L -> "${rem / 1_000_000L}백만"
                rem >= 100_000L -> "${rem / 100_000L}십만"
                rem >= 10_000L -> "${rem / 10_000L}만"
                else -> ""
            }
            "${eok}억$second"
        }
    }
    return "$sign$body"
}
