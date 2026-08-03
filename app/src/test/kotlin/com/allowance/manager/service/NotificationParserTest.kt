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
    fun `한 자리 금액(1원)도 인식 - 실제 KB 입금 알림`() {
        val result = NotificationParser.parse(
            packageName = "com.kbstar.kbbank",
            title = "입금 1원",
            text = "이*름님 07/20 00:15 941602-**-***318 이푸름 FBS입금 1 잔액26,690",
        )
        requireNotNull(result)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(1L, result.amount)
        assertEquals(26_690L, result.balance)
        assertTrue(result.extractedAccount?.contains("941602") == true)
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

    @Test
    fun `Layer2 - 키워드+맨숫자 노이즈(메신저)는 null`() {
        // 원 단위·잔액·계좌번호가 전혀 없는 맨숫자 → 돈 형태 아님 → 드롭
        val result = NotificationParser.parse(
            packageName = "com.Slack",
            title = "proj_abc_waas: [ABC]",
            text = "승인 요청 1건 확인해주세요",
        )
        assertNull(result)
    }

    @Test
    fun `Layer2 - 원 단위 금액은 어떤 앱이어도 인정(화이트리스트 없음)`() {
        // 화이트리스트를 없앴으므로 등록 안 된 앱이라도 '원' 금액이면 인정
        val result = NotificationParser.parse(
            packageName = "com.unknown.newbank",
            title = "결제",
            text = "스타벅스 4,500원 승인",
        )
        requireNotNull(result)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(4_500L, result.amount)
    }

    @Test
    fun `Layer2 - 원은 없지만 잔액 동반하면 키워드 뒤 숫자 인정`() {
        val result = NotificationParser.parse(
            packageName = "com.kbstar.kbbank",
            title = "출금 3000",
            text = "체크카드 출금 3000 잔액 12,000",
        )
        requireNotNull(result)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(3_000L, result.amount)
    }
}
