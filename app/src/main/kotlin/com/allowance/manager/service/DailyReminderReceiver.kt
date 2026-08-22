package com.allowance.manager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.allowance.manager.MainActivity
import com.allowance.manager.R
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import com.allowance.manager.core.domain.usecase.alert.DailyReminderDecider
import com.allowance.manager.core.domain.usecase.alert.GetDailyReminderSettingUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 매일 지정 시각에 발화 — "안 본 내역"이 있으면 가계부 확인 알림을 보낸다.
 * (마지막 앱 조회·마지막 알림 이후 새 내역이 있을 때만: [DailyReminderDecider])
 */
@AndroidEntryPoint
class DailyReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var getDailyReminderSettingUseCase: GetDailyReminderSettingUseCase
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var dataStoreRepository: DataStoreRepository

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DailyReminderScheduler.ACTION_DAILY_REMINDER) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val setting = getDailyReminderSettingUseCase().first()
                if (!setting.enabled) return@launch

                val latest = transactionRepository.getLastTransactionTime()
                val lastSeen = dataStoreRepository.getReminderLastSeen()
                val lastNotified = dataStoreRepository.getReminderLastNotified()

                if (DailyReminderDecider.shouldRemind(latest, lastSeen, lastNotified)) {
                    notify(context)
                    dataStoreRepository.setReminderLastNotified(System.currentTimeMillis())
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context) {
        createChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_piggy)
            .setContentTitle("오늘 안 본 내역이 있어요")
            .setContentText("확인하고 정리해볼까요?")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { manager.notify(NOTIF_ID, notification) }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "가계부 관리 알림",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "그날 안 본 내역이 있으면 확인하라고 알려드려요" }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "daily_reminder"
        private const val NOTIF_ID = 1003
    }
}
