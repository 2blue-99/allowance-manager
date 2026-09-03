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

    // ─────────── 사용처(merchant) 추출 — 실제 은행·카드 알림 원문 기반 ───────────

    @Test
    fun `사용처 - KB 푸시 출금(계좌 뒤 상대처)`() {
        val result = NotificationParser.parse(
            packageName = "com.kbstar.kbbank",
            title = "출금 8,110원",
            text = "이*름님 09/03 16:46 941602-**-***318 쿠팡 FBS출금 8,110 잔액77,139",
        )
        requireNotNull(result)
        assertEquals(8_110L, result.amount)
        assertEquals("쿠팡", result.merchant)
    }

    @Test
    fun `사용처 - KB 푸시 입금은 보낸 사람 이름`() {
        val result = NotificationParser.parse(
            packageName = "com.kbstar.kbbank",
            title = "입금 1,000원",
            text = "이*름님 04/01 14:48 941602-**-***318 이푸름 FBS입금 1,000 잔액225,023",
        )
        requireNotNull(result)
        assertEquals("이푸름", result.merchant)
    }

    @Test
    fun `사용처 - KB 체크카드 문자(Web발신 접두)`() {
        val result = NotificationParser.parse(
            packageName = "com.samsung.android.messaging",
            title = null,
            text = "[Web발신] [KB]11/18 14:58 279801**027 과일놀이터 체크카드출금 18,000 잔액238,281",
        )
        requireNotNull(result)
        assertEquals(18_000L, result.amount)
        assertEquals("과일놀이터", result.merchant)
    }

    @Test
    fun `사용처 - 우리은행식(금액 뒤 · 잔액 앞)`() {
        val result = NotificationParser.parse(
            packageName = "com.samsung.android.messaging",
            title = null,
            text = "[Web발신] 우리 11/17 18:17 *250284 출금 3,000원 서울특별시－현 잔액 214,164원",
        )
        requireNotNull(result)
        assertEquals(3_000L, result.amount)
        assertEquals("서울특별시－현", result.merchant)
    }

    @Test
    fun `사용처 - 농협식(계좌 뒤 · 잔액 앞)`() {
        val result = NotificationParser.parse(
            packageName = "com.samsung.android.messaging",
            title = null,
            text = "[Web발신] 농협 출금2,500원 11/03 17:43 301-****-2640-41 파우PC 잔액5,428원",
        )
        requireNotNull(result)
        assertEquals(2_500L, result.amount)
        assertEquals("파우PC", result.merchant)
    }

    @Test
    fun `사용처 - 현대카드식(맨 끝 상대처)`() {
        val result = NotificationParser.parse(
            packageName = "com.samsung.android.messaging",
            title = null,
            text = "[Web발신] 2024/11/15 12:57 출금 354,594원 잔액 50,622원 현대카드 469***03801011 기업",
        )
        requireNotNull(result)
        assertEquals(354_594L, result.amount)
        assertEquals("기업", result.merchant)
    }

    @Test
    fun `사용처 - 카드 승인 알림(금액 뒤 가맹점)`() {
        val result = NotificationParser.parse(
            packageName = "com.shinhancard.smartcalculator",
            title = "신한카드 승인",
            text = "홍길동님 6,500원 스타벅스 결제",
        )
        requireNotNull(result)
        assertEquals("스타벅스", result.merchant)
    }

    @Test
    fun `사용처 - 뽑을 게 없으면 null(출처명 폴백)`() {
        val result = NotificationParser.parse(
            packageName = "com.kbstar.kbbank",
            title = "출금 3000",
            text = "체크카드 출금 3000 잔액 12,000",
        )
        requireNotNull(result)
        assertNull(result.merchant)
    }
}
