package com.allowance.manager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.allowance.manager.core.domain.usecase.alert.GetDailyReminderSettingUseCase
import com.allowance.manager.core.domain.usecase.alert.GetPaydayAlertSettingUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 재부팅 후 상태바 서비스를 재시작하고, 가계부 관리 알림·월급일 알림(매일 알람)을 재예약한다.
 * (알람은 재부팅 시 소실되므로 여기서 다시 건다. 포그라운드 시작 제한은 best-effort)
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var getDailyReminderSettingUseCase: GetDailyReminderSettingUseCase
    @Inject lateinit var dailyReminderScheduler: DailyReminderScheduler
    @Inject lateinit var getPaydayAlertSettingUseCase: GetPaydayAlertSettingUseCase
    @Inject lateinit var paydayAlarmScheduler: PaydayAlarmScheduler

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        StatusBarService.start(context)

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val setting = getDailyReminderSettingUseCase().first()
                if (setting.enabled) dailyReminderScheduler.schedule(setting.hour, setting.minute)
                if (getPaydayAlertSettingUseCase().first().enabled) paydayAlarmScheduler.schedule()
            } finally {
                pending.finish()
            }
        }
    }
}
