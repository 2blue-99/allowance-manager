package com.allowance.manager.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java)
    }

    private val recordTransactionUseCase get() = entryPoint.recordTransactionUseCase()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification?.extras ?: return

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

        Timber.d("알림수신 pkg=${sbn.packageName} title=$title body=$body")

        val result = NotificationParser.parse(
            packageName = sbn.packageName,
            title = title,
            text = body,
        ) ?: run {
            Timber.d("파싱 실패(무시) pkg=${sbn.packageName}")
            return
        }
        Timber.d("파싱 성공 type=${result.type} amount=${result.amount} account=${result.extractedAccount}")

        serviceScope.launch {
            runCatching {
                recordTransactionUseCase(
                    ParsedTransaction(
                        type = result.type,
                        amount = result.amount,
                        balance = result.balance,
                        packageName = sbn.packageName,
                        sourceName = result.sourceName,
                        extractedAccount = result.extractedAccount,
                        rawText = result.content,
                    )
                )
            }.onFailure { Timber.e(it, "거래 저장 실패") }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
