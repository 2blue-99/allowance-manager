package com.allowance.manager.core.domain.usecase.alert

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyReminderDeciderTest {

    @Test
    fun `내역이 없으면 발송 안 함`() {
        assertFalse(DailyReminderDecider.shouldRemind(latestTxCreatedAt = null, lastSeen = 0, lastNotified = 0))
    }

    @Test
    fun `마지막 조회 이후 새 내역이 있으면 발송`() {
        assertTrue(DailyReminderDecider.shouldRemind(latestTxCreatedAt = 200, lastSeen = 100, lastNotified = 0))
    }

    @Test
    fun `이미 조회한(앱 열어본) 내역이면 발송 안 함`() {
        assertFalse(DailyReminderDecider.shouldRemind(latestTxCreatedAt = 100, lastSeen = 150, lastNotified = 0))
    }

    @Test
    fun `이미 알린 배치면 재발송 안 함`() {
        assertFalse(DailyReminderDecider.shouldRemind(latestTxCreatedAt = 100, lastSeen = 0, lastNotified = 120))
    }

    @Test
    fun `알린 뒤 더 새로운 내역이 오면 다시 발송`() {
        assertTrue(DailyReminderDecider.shouldRemind(latestTxCreatedAt = 300, lastSeen = 0, lastNotified = 200))
    }
}
