package com.allowance.manager.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeduperTest {

    @Test
    fun `같은 키·시각·내용이 다시 오면 무시한다 (삼성 자동 그룹 중복 전달)`() {
        val d = NotificationDeduper()
        assertTrue(d.shouldProcess("0|com.kakao.talk|2|tag|10355", 1_000L, "송금 5,000원"))
        assertFalse(d.shouldProcess("0|com.kakao.talk|2|tag|10355", 1_000L, "송금 5,000원"))
    }

    @Test
    fun `내용이 바뀐 갱신은 새 알림으로 통과시킨다`() {
        val d = NotificationDeduper()
        assertTrue(d.shouldProcess("k", 1_000L, "처리 중"))
        assertTrue(d.shouldProcess("k", 1_000L, "완료 50,000원"))
    }

    @Test
    fun `게시 시각이 다르면(재게시) 통과시킨다`() {
        val d = NotificationDeduper()
        assertTrue(d.shouldProcess("k", 1_000L, "결제 5,000원"))
        assertTrue(d.shouldProcess("k", 2_000L, "결제 5,000원"))
    }

    @Test
    fun `용량을 넘기면 가장 오래된 것부터 잊는다`() {
        val d = NotificationDeduper(capacity = 2)
        d.shouldProcess("a", 1L, "x"); d.shouldProcess("b", 1L, "x"); d.shouldProcess("c", 1L, "x")
        assertTrue(d.shouldProcess("a", 1L, "x"))     // 밀려나서 다시 처음 보는 것으로
        assertFalse(d.shouldProcess("c", 1L, "x"))    // 최근 것은 기억
    }
}
