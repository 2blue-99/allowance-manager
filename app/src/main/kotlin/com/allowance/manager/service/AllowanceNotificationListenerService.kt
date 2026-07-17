package com.allowance.manager.service

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
        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text")?.toString()

        val result = NotificationParser.parse(
            packageName = sbn.packageName,
            title = title,
            text = text,
        ) ?: return

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
