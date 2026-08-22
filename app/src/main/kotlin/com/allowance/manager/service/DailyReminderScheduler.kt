package com.allowance.manager.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 가계부 관리 알림(매일 정산 리마인더) 스케줄러.
 * 정확 알람이 필요 없는 리마인더라 [AlarmManager.setInexactRepeating]으로 매일 지정 시각에 반복 예약한다.
 * (재부팅 시 알람이 소실되므로 [BootReceiver]에서 재예약)
 */
@Singleton
class DailyReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule(hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTriggerAt(hour, minute),
            AlarmManager.INTERVAL_DAY,
            pendingIntent(),
        )
    }

    fun cancel() {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent())
    }

    // 오늘 hh:mm이 이미 지났으면 내일 hh:mm으로.
    private fun nextTriggerAt(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour.coerceIn(0, 23)).withMinute(minute.coerceIn(0, 59)).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.toInstant().toEpochMilli()
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, DailyReminderReceiver::class.java).setAction(ACTION_DAILY_REMINDER),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val ACTION_DAILY_REMINDER = "com.allowance.manager.action.DAILY_REMINDER"
        private const val REQUEST_CODE = 2001
    }
}
