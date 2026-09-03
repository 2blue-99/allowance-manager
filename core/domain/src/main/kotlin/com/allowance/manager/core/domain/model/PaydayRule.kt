package com.allowance.manager.core.domain.model

import java.time.LocalDate

/**
 * 월급일 이력 한 건 — **"[effectiveDate]에 받았고, 그 이후 규칙은 [payday]"**.
 *
 * 사이클 시작 경계는 [effectiveDate] 그대로 쓰고(사용자가 캘린더에서 찍은 사실이므로 보정하지 않는다),
 * 다음 경계는 [payday]를 영업일 보정해서 계산한다. — [BudgetCycle] 참고.
 *
 * @param payday 1~31, 0 = 말일
 */
data class PaydayRule(
    val effectiveDate: LocalDate,
    val payday: Int,
) {
    companion object {
        /** 이력 보존 개수. 넘으면 오래된 것부터 버린다. */
        const val MAX_ENTRIES = 24
    }
}
