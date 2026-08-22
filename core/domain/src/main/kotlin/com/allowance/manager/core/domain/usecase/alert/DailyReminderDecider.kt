package com.allowance.manager.core.domain.usecase.alert

/**
 * 가계부 관리 알림 발송 여부 판정(순수 로직).
 *
 * "안 본 내역"이 있을 때만 발송:
 * - 최신 내역이 **마지막 앱 조회(lastSeen)** 이후 기록됐고,
 * - 그 내역에 대해 **아직 알림을 안 보냈을 때(lastNotified)**.
 * → 이미 확인했거나(앱 열어봄) 이미 알린 배치면 재발송하지 않는다.
 */
object DailyReminderDecider {
    fun shouldRemind(latestTxCreatedAt: Long?, lastSeen: Long, lastNotified: Long): Boolean {
        if (latestTxCreatedAt == null) return false
        return latestTxCreatedAt > maxOf(lastSeen, lastNotified)
    }
}
