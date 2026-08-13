package com.allowance.manager.core.domain.model

/**
 * 소비/수입 분류. 바텀시트에서 사용자가 직접 지정한다(선택).
 * label = 표시 이름, emoji = 뱃지·리스트 아이콘.
 */
enum class TransactionCategory(val label: String, val emoji: String) {
    FOOD("식비", "🍚"),
    CAFE("카페·간식", "☕"),
    TRANSPORT("교통", "🚌"),
    SHOPPING("쇼핑", "🛍️"),
    FASHION("패션·의류", "👗"),
    BEAUTY("뷰티·미용", "💄"),
    LIVING("생활", "🧻"),
    HEALTH("의료·건강", "💊"),
    CULTURE("문화·취미", "🎬"),
    EDUCATION("교육", "📚"),
    EVENT("경조사·선물", "🎁"),
    HOUSING("주거·통신", "🏠"),
    INCOME("수입", "💰"),
    ETC("기타", "📌"),
}
