package com.allowance.manager.feature.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * 날짜가 바뀌면 위젯 4종을 다시 그린다 — 수급일이 지나면 새 사이클, 자정이 지나면 '오늘' 지표로.
 *
 * 위젯은 주기 갱신이 꺼져 있고(updatePeriodMillis=0) 데이터 변화 때만 갱신되므로,
 * 거래가 없는 날엔 지난 사이클·어제 값이 그대로 남는다. 앱이 꺼져 있어도 맞도록 알람으로 깨운다.
 *
 * 알람은 비-wakeup(RTC): 화면이 꺼져 있으면 위젯도 안 보이니, 기기가 깨어날 때 한 번 돌면 충분하다.
 * 한 번 울리면 다음 자정을 다시 건다. 재부팅·프로세스 재시작은 Application.onCreate의 예약이 커버.
 */
fun scheduleWidgetDayRefresh(context: Context) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.set(AlarmManager.RTC, nextMidnightMillis(), dayRefreshIntent(context))
}

/** 다음 자정 + 여유 5초 (경계 직후 LocalDate.now()가 확실히 새 날이 되도록) */
private fun nextMidnightMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + MIDNIGHT_SLACK_MS

private fun dayRefreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context,
    REQUEST_CODE,
    Intent(context, WidgetDayChangeReceiver::class.java).setAction(ACTION_DAY_REFRESH),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

/** 자정 알람 + 사용자가 시각·시간대를 바꿨을 때 위젯 갱신 후 다음 자정 재예약 */
class WidgetDayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                runCatching { refreshBalanceWidget(context) }
                runCatching { scheduleWidgetDayRefresh(context) }
            } finally {
                pending.finish()
            }
        }
    }
}

private const val ACTION_DAY_REFRESH = "com.allowance.manager.action.WIDGET_DAY_REFRESH"
private const val REQUEST_CODE = 3003
private const val MIDNIGHT_SLACK_MS = 5_000L
