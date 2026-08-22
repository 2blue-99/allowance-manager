package com.allowance.manager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.allowance.manager.MainActivity
import com.allowance.manager.R
import com.allowance.manager.core.domain.model.BudgetAlertSetting
import com.allowance.manager.core.domain.model.BudgetAlertState
import com.allowance.manager.core.domain.model.BudgetStatus
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.usecase.alert.BudgetAlertEvaluator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 예산 소진 알림 발송기.
 * [ObserveBudgetStatusUseCase] 변화(= 메인 계좌 지출/수입/예산 변동)를 받아 임계값 관통 시 알림을 쏜다.
 * - 한 번에 여러 임계값 관통 시 가장 심각한 것 하나만 발송, 회복 시 재장전([BudgetAlertEvaluator]).
 * - 발송 상태는 DataStore에 저장해 프로세스 재시작에도 중복 발송을 막는다.
 */
@Singleton
class BudgetAlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreRepository: DataStoreRepository,
) {
    /** BudgetStatus 한 건을 받아 임계값을 평가하고 필요 시 알림을 발송한다. (단일 콜렉터에서 순차 호출) */
    suspend fun onBudgetStatus(status: BudgetStatus, setting: BudgetAlertSetting, userType: UserType) {
        if (!setting.enabled || status.budget <= 0) return

        val cycleStart = status.cycle.startMillis()
        val saved = dataStoreRepository.getBudgetAlertState()
        // 사이클이 바뀌면 이전 발송 기록은 무시(초기화)
        val fired = if (saved.cycleStart == cycleStart) saved.fired else emptySet()

        val remainingPercent = status.remaining * 100f / status.budget
        val result = BudgetAlertEvaluator.evaluate(
            remainingPercent = remainingPercent,
            thresholds = setting.frequency.remainingThresholds,
            fired = fired,
        )

        if (result.newFired != fired || saved.cycleStart != cycleStart) {
            dataStoreRepository.setBudgetAlertState(BudgetAlertState(cycleStart, result.newFired))
        }
        result.thresholdToFire?.let { notify(it, userType) }
    }

    private fun notify(threshold: Int, userType: UserType) {
        createChannel()
        val (title, body) = copyFor(threshold, userType)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_piggy)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        // POST_NOTIFICATIONS 미허용 등으로 실패해도 앱 흐름은 유지
        runCatching { manager().notify(NOTIF_ID, notification) }
    }

    // 남은 % 임계값별 문구 (0 = 다 씀). 호칭(용돈/생활비/예산)은 사용자 유형을 따른다.
    private fun copyFor(threshold: Int, type: UserType): Pair<String, String> {
        val label = type.label                          // 용돈 / 생활비 / 예산
        val subject = "$label${type.subjectParticle}"   // 용돈이 / 생활비가 / 예산이
        val obj = "$label${type.objectParticle}"        // 용돈을 / 생활비를 / 예산을
        return when {
            threshold <= 0 -> "$obj 다 썼어요 😢" to "$label 초과예요. 지출 계획을 수정해볼까요?"
            threshold <= 20 -> "$subject ${threshold}% 남았어요 🚨" to "곧 바닥나요. 꼭 필요한 지출만!"
            threshold < 50 -> "$subject ${threshold}% 남았어요 ⚠️" to "슬슬 아껴볼까요?"
            threshold == 50 -> "$subject ${threshold}% 남았어요 😊" to "페이스 조절 잘 하고 계신가요?"
            else -> "$subject ${threshold}% 남았어요 👍" to "건강한 소비습관, 내돈지켜!"
        }
    }

    private fun manager() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "예산 소진 알림",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "예산이 줄어들 때 알려드려요" }
            manager().createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "budget_alert"
        private const val NOTIF_ID = 1002
    }
}
