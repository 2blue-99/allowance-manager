package com.allowance.manager.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.allowance.manager.core.domain.model.PaydayAlertSetting
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 월급일 알림 스케줄러 — 매일 오후 9시에 한 번 깨워서 [PaydayAlarmReceiver]가 그날 알릴지 판정한다.
 *
 * "지급일 전날/당일에만 알람을 건다"가 아니라 **매일 깨워서 그날 판정**하는 방식이다.
 * 월급일·조정값·공휴일이 언제 바뀌어도 다음 발화 때 자동으로 맞고, 재예약 로직이 필요 없다.
 * (재부팅 시 알람이 소실되므로 [BootReceiver]에서 재예약)
 */
@Singleton
class PaydayAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule() {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTriggerAt(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent(),
        )
    }

    fun cancel() {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent())
    }

    // 오늘 오후 9시가 이미 지났으면 내일 오후 9시로.
    private fun nextTriggerAt(): Long {
        val now = ZonedDateTime.now()
        var next = now
            .withHour(PaydayAlertSetting.HOUR)
            .withMinute(PaydayAlertSetting.MINUTE)
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.toInstant().toEpochMilli()
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, PaydayAlarmReceiver::class.java).setAction(ACTION_PAYDAY_ALERT),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val ACTION_PAYDAY_ALERT = "com.allowance.manager.action.PAYDAY_ALERT"

        /** 디버그 화면 전용 — 판정·중복검사를 건너뛰고 지정한 종류를 바로 발송("TOMORROW"/"TODAY") */
        const val EXTRA_FORCE_NOTICE = "force_notice"

        private const val REQUEST_CODE = 2002
    }
}
