package com.allowance.manager.core.domain.usecase.alert

import com.allowance.manager.core.domain.model.Holidays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PaydayNoticeDeciderTest {

    @Test
    fun `지급일 당일이면 TODAY`() {
        // 2026-07-25는 토요일 → 실지급일 7/24(금)
        val notice = PaydayNoticeDecider.decide(today = LocalDate.of(2026, 7, 24), payday = 25)
        assertEquals(PaydayNoticeDecider.Notice.TODAY, notice)
    }

    @Test
    fun `실지급일 하루 전이면 TOMORROW`() {
        val notice = PaydayNoticeDecider.decide(today = LocalDate.of(2026, 7, 23), payday = 25)
        assertEquals(PaydayNoticeDecider.Notice.TOMORROW, notice)
    }

    @Test
    fun `명목 지급일에는 이미 지나서 알림이 없다`() {
        // 7/25(토)는 실지급일이 아니라 보정 대상 → 알림 없음
        val notice = PaydayNoticeDecider.decide(today = LocalDate.of(2026, 7, 25), payday = 25)
        assertEquals(PaydayNoticeDecider.Notice.NONE, notice)
    }

    @Test
    fun `공휴일 보정도 실지급일에 반영된다`() {
        // 8/25(화)를 공휴일로 지정 → 실지급일 8/24(월), 그 전날은 8/23
        val holidays = Holidays(mapOf(LocalDate.of(2026, 8, 25) to "임시공휴일"))
        assertEquals(
            PaydayNoticeDecider.Notice.TOMORROW,
            PaydayNoticeDecider.decide(LocalDate.of(2026, 8, 23), payday = 25, holidays = holidays),
        )
        assertEquals(
            PaydayNoticeDecider.Notice.TODAY,
            PaydayNoticeDecider.decide(LocalDate.of(2026, 8, 24), payday = 25, holidays = holidays),
        )
    }

    @Test
    fun `달을 넘어가는 지급일도 잡는다`() {
        // 규칙일 1일, 2026-11-01은 일요일 → 실지급일 10/30(금)
        assertEquals(
            PaydayNoticeDecider.Notice.TODAY,
            PaydayNoticeDecider.decide(LocalDate.of(2026, 10, 30), payday = 1),
        )
        // 규칙일 1일, 2026-09-01(화)는 그대로 → 8/31(월) 밤에 "내일"
        assertEquals(
            PaydayNoticeDecider.Notice.TOMORROW,
            PaydayNoticeDecider.decide(LocalDate.of(2026, 8, 31), payday = 1),
        )
    }

    @Test
    fun `말일 규칙도 판정된다`() {
        // payday = 0 → 말일. 2026-08-31은 월요일
        assertEquals(
            PaydayNoticeDecider.Notice.TODAY,
            PaydayNoticeDecider.decide(LocalDate.of(2026, 8, 31), payday = 0),
        )
    }

    @Test
    fun `지급일과 무관한 날은 NONE`() {
        assertEquals(
            PaydayNoticeDecider.Notice.NONE,
            PaydayNoticeDecider.decide(LocalDate.of(2026, 8, 10), payday = 25),
        )
    }
}
