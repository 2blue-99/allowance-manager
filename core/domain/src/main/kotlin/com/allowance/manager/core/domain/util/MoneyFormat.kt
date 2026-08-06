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
