package com.allowance.manager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.allowance.manager.BuildConfig
import com.allowance.manager.MainActivity
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.usecase.budget.GetUserTypeUseCase
import com.allowance.manager.core.domain.usecase.budget.ObserveBudgetStatusUseCase
import com.allowance.manager.core.domain.usecase.setting.GetStatusBarEnabledUseCase
import com.allowance.manager.core.domain.util.amountToComma
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 이번달 지출/예산을 상태바에 상시 표시하는 포그라운드 서비스 (토스 만보기 방식).
 * STATUS_BAR_ENABLED = false 이면 스스로 종료.
 */
@AndroidEntryPoint
class StatusBarService : Service() {

    @Inject lateinit var observeBudgetStatusUseCase: ObserveBudgetStatusUseCase
    @Inject lateinit var getStatusBarEnabledUseCase: GetStatusBarEnabledUseCase
    @Inject lateinit var getUserTypeUseCase: GetUserTypeUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** 사용자가 밀어서 지운 알림을 다시 띄우기 위한, 마지막으로 게시한 알림. */
    private var lastNotification: Notification? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Android 14+ 에선 setOngoing 이어도 FGS 알림을 밀어서 지울 수 있다.
     * 지우면 시스템이 deleteIntent 를 쏘고, 이를 받아 [REPOST_DELAY_MS] 뒤에 재게시한다. (토스 만보기 방식)
     */
    private val repostReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            mainHandler.postDelayed(
                { lastNotification?.let { notificationManager().notify(NOTIF_ID, it) } },
                REPOST_DELAY_MS,
            )
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ContextCompat.registerReceiver(
            this,
            repostReceiver,
            IntentFilter(ACTION_REPOST),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        startForeground(NOTIF_ID, buildNotification(0L, 0L, isOver = false, label = UserType.Default.label))

        scope.launch {
            combine(
                observeBudgetStatusUseCase(),
                getStatusBarEnabledUseCase(),
                getUserTypeUseCase(),
            ) { status, enabled, userType -> Triple(status, enabled, userType) }
                .collect { (status, enabled, userType) ->
                    if (!enabled) {
                        stopSelf()
                        return@collect
                    }
                    notificationManager().notify(
                        NOTIF_ID,
                        buildNotification(status.spent, status.budget, status.isOver, userType.label),
                    )
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(repostReceiver) }
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(spent: Long, budget: Long, isOver: Boolean, label: String): Notification {
        val content = if (budget > 0) {
            "이번달 지출 ${spent.amountToComma()}원 / ${budget.amountToComma()}원"
        } else {
            "이번달 지출 ${spent.amountToComma()}원"
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val deleteIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_REPOST).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(if (isOver) "이번달 $label 초과" else "내돈지켜")
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            .also { lastNotification = it }
    }

    private fun notificationManager() =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "이번달 지출 현황",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "상태바에 이번달 지출 현황을 상시 표시"
                setShowBadge(false)
            }
            notificationManager().createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "status_bar_balance"
        private const val NOTIF_ID = 1001
        private const val ACTION_REPOST = "com.allowance.manager.action.STATUS_BAR_REPOST"

        /** 밀어서 지운 뒤 다시 띄우기까지의 지연. debug=5초(테스트 편의), release=30초. */
        private val REPOST_DELAY_MS = if (BuildConfig.DEBUG) 5_000L else 30_000L

        /** 상태바 서비스 시작 (포그라운드). 백그라운드 시작 제한 예외는 무시. */
        fun start(context: Context) {
            val intent = Intent(context, StatusBarService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }
}
