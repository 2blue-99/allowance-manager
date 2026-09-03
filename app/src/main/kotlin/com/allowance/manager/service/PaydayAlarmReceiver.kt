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
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.usecase.alert.GetPaydayAlertSettingUseCase
import com.allowance.manager.core.domain.usecase.alert.PaydayNoticeDecider
import com.allowance.manager.core.domain.util.amountToComma
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 매일 오후 9시에 발화 — 오늘이 실지급일 전날/당일이면 월급일 알림을 보낸다.
 *
 * 판정은 [PaydayNoticeDecider]가 하고, 실지급일 계산은 사이클 경계와 같은 곳
 * ([com.allowance.manager.core.domain.model.BudgetCycle.payDate])에서 나온다 → 홈의 D-day와 항상 일치.
 */
@AndroidEntryPoint
class PaydayAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var getPaydayAlertSettingUseCase: GetPaydayAlertSettingUseCase
    @Inject lateinit var dataStoreRepository: DataStoreRepository
    @Inject lateinit var remoteConfigRepository: RemoteConfigRepository
    @Inject lateinit var cycleRepository: CycleRepository

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != PaydayAlarmScheduler.ACTION_PAYDAY_ALERT) return
        val forced = intent.getStringExtra(PaydayAlarmScheduler.EXTRA_FORCE_NOTICE)
            ?.let { name -> runCatching { PaydayNoticeDecider.Notice.valueOf(name) }.getOrNull() }

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val userType = dataStoreRepository.getUserType().first()

                // 디버그 강제 발송 — 설정·판정·중복검사를 건너뛰고 문구만 확인한다. 발송 표식도 남기지 않는다.
                if (forced != null) {
                    if (forced != PaydayNoticeDecider.Notice.NONE) notify(context, forced, userType)
                    return@launch
                }

                if (!getPaydayAlertSettingUseCase().first().enabled) return@launch

                val today = LocalDate.now()
                val notice = PaydayNoticeDecider.decide(
                    today = today,
                    // 현재 규칙 = 최신 사이클 행의 payday (단일 소스)
                    payday = cycleRepository.cycleAt(today).payday,
                    holidays = remoteConfigRepository.getHolidays(),
                )
                if (notice == PaydayNoticeDecider.Notice.NONE) return@launch

                // 같은 날 같은 종류는 한 번만. (알람 재예약·부팅 등으로 하루에 두 번 발화할 수 있다)
                val stamp = "$today:$notice"
                if (dataStoreRepository.getPaydayAlertLastSent() == stamp) return@launch

                notify(context, notice, userType)
                dataStoreRepository.setPaydayAlertLastSent(stamp)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun notify(context: Context, notice: PaydayNoticeDecider.Notice, userType: UserType) {
        val (title, body) = when (notice) {
            PaydayNoticeDecider.Notice.TOMORROW ->
                "내일은 ${userType.paydayShort}이에요!" to
                    "이번 달 ${userType.paydayShort}을 바꾸고 싶으면 설정에서 조정할 수 있어요."
            PaydayNoticeDecider.Notice.TODAY ->
                "이번 달 ${userType.label} 변동이 있으면 수정해주세요!" to lastCycleBudgetText()
            PaydayNoticeDecider.Notice.NONE -> return
        }
        post(context, notice, title, body)
    }

    /**
     * "지난번엔 600,000원이었어요." — 이력이 없으면(설치 첫 사이클) null을 주고 제목만 보낸다.
     *
     * 기준이 사이클인 이유: 사용자가 이 알림을 보고 바꾸는 예산도 `SetMonthlyBudgetUseCase`가
     * 현재 사이클에 쓰므로, 비교 대상도 같은 기준이어야 말이 맞다.
     */
    private suspend fun lastCycleBudgetText(): String? {
        // 현재 사이클 시작 하루 전 = 직전 사이클 안의 어느 날
        val previousDay = cycleRepository.cycleAt().start.minusDays(1)
        val amount = cycleRepository.cycleAt(previousDay).budget
        return if (amount > 0) "지난번엔 ${amount.amountToComma()}원이었어요." else null
    }

    private fun post(context: Context, notice: PaydayNoticeDecider.Notice, title: String, body: String?) {
        createChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_piggy)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (body != null) {
            builder.setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 전날/당일을 다른 ID로 — 전날 알림을 안 지운 채 당일 알림이 와도 덮어쓰지 않는다.
        val id = if (notice == PaydayNoticeDecider.Notice.TODAY) NOTIF_ID_TODAY else NOTIF_ID_TOMORROW
        runCatching { manager.notify(id, builder.build()) }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "월급일 알림",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "월급일 전날과 당일에 알려드려요" }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "payday_alert"
        private const val NOTIF_ID_TOMORROW = 1004
        private const val NOTIF_ID_TODAY = 1005
    }
}
