package com.allowance.manager.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.AnalyticsHelper
import com.allowance.manager.core.domain.model.ParsedTransaction
import com.allowance.manager.core.domain.usecase.transaction.RecordTransactionUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

class AllowanceNotificationListenerService : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceEntryPoint {
        fun recordTransactionUseCase(): RecordTransactionUseCase
        fun analyticsHelper(): AnalyticsHelper
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java)
    }

    private val recordTransactionUseCase get() = entryPoint.recordTransactionUseCase()

    private val analyticsHelper get() = entryPoint.analyticsHelper()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun log(message: String) = Timber.tag(TAG).d(message)

    override fun onCreate() {
        super.onCreate()
        log("서비스 onCreate")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        analyticsHelper.logEvent(AmAnalytics.Event.NOTI_PERMISSION_GRANTED)
        log("리스너 연결됨 (알림 접근 권한 OK) — 이제 알림을 수신합니다")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        analyticsHelper.logEvent(AmAnalytics.Event.NOTI_PERMISSION_REVOKED)
        log("리스너 연결 해제됨 (권한 꺼짐 or 재바인딩 필요)")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification?.extras
        if (extras == null) {
            log("알림수신 pkg=${sbn.packageName} (extras 없음 → 무시)")
            return
        }

        // CharSequence로 저장된 제목/본문도 안전하게 추출 (getString은 null 반환됨)
        fun field(key: String): String? =
            extras.getCharSequence(key)?.toString()?.takeIf { it.isNotBlank() }

        val title = field(Notification.EXTRA_TITLE)
        // 짧은 text + 펼친 bigText + subText/infoText 모두 합쳐 파싱 (금액이 bigText에만 있는 경우 대비)
        val body = listOfNotNull(
            field(Notification.EXTRA_TEXT),
            field(Notification.EXTRA_BIG_TEXT),
            field(Notification.EXTRA_SUB_TEXT),
            field(Notification.EXTRA_INFO_TEXT),
        ).distinct().joinToString(" ")

        log("알림수신 pkg=${sbn.packageName}\n  title=$title\n  body=$body")

        // 파싱은 어떤 알림 문구가 와도 서비스가 죽지 않게 방어 (실패 = 무시)
        val result = runCatching {
            NotificationParser.parse(
                packageName = sbn.packageName,
                title = title,
                text = body,
            )
        }.onFailure { Timber.tag(TAG).e(it, "파싱 중 예외") }.getOrNull() ?: run {
            log("→ 파싱 실패(무시): 금전 키워드/금액을 못 찾음")
            return
        }
        // 사용처(merchant)는 개인정보 → 값 대신 추출 여부만 로그
        log("→ 파싱 성공 type=${result.type} amount=${result.amount} account=${result.extractedAccount} bank=${result.sourceName} merchant=${result.merchant != null}")

        serviceScope.launch {
            runCatching {
                val id = recordTransactionUseCase(
                    ParsedTransaction(
                        type = result.type,
                        amount = result.amount,
                        balance = result.balance,
                        packageName = sbn.packageName,
                        sourceName = result.sourceName,
                        merchant = result.merchant,
                        extractedAccount = result.extractedAccount,
                        rawText = result.content,
                    )
                )
                if (id == null) {
                    analyticsHelper.logEvent(AmAnalytics.Event.TX_AUTO_IGNORED)
                    log("→ 무시 계좌 매칭 → 드롭(저장 안 함)")
                } else {
                    analyticsHelper.logEvent(AmAnalytics.Event.TX_AUTO_RECORDED, mapOf(AmAnalytics.Param.TYPE to result.type.name.lowercase()))
                    log("→ 저장 완료")
                }
            }.onFailure { Timber.tag(TAG).e(it, "거래 저장 실패") }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "NOTI"
    }
}
