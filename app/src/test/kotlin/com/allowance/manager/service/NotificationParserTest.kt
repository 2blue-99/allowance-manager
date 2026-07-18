package com.allowance.manager.service

import com.allowance.manager.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationParserTest {

    @Test
    fun `입금 알림 - 금액·잔액·계좌·타입 파싱`() {
        val result = NotificationParser.parse(
            packageName = "com.kbstar.kbbank",
            title = "입금 1,000원",
            text = "이*름님 04/01 14:48 941602-**-***318 이푸름 FBS입금 1,000 잔액225,023",
        )
        requireNotNull(result)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(1_000L, result.amount)
        assertEquals(225_023L, result.balance)
        assertEquals("KB국민은행", result.sourceName)
        assertTrue(result.extractedAccount?.contains("941602") == true)
    }

    @Test
    fun `카드 결제 알림 - 지출로 파싱`() {
        val result = NotificationParser.parse(
            packageName = "com.shinhancard.smartcalculator",
            title = "신한카드 승인",
            text = "홍길동님 6,500원 스타벅스 결제",
        )
        requireNotNull(result)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(6_500L, result.amount)
    }

    @Test
    fun `취소·환불 알림 - 마이너스 지출로 파싱`() {
        val result = NotificationParser.parse(
            packageName = "com.shinhancard.smartcalculator",
            title = "신한카드",
            text = "승인취소 6,500원 스타벅스",
        )
        requireNotNull(result)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(-6_500L, result.amount)
    }

    @Test
    fun `금전 키워드 없는 노이즈 알림 - null`() {
        val result = NotificationParser.parse(
            packageName = "com.example.sms",
            title = "인증번호",
            text = "[Web발신] 인증번호 123456 입니다",
        )
        assertNull(result)
    }

    @Test
    fun `금액 없는 알림 - null`() {
        val result = NotificationParser.parse(
            packageName = "com.kbstar.kbbank",
            title = "입금 안내",
            text = "입금이 완료되었습니다",
        )
        assertNull(result)
    }
}
