package com.allowance.manager.service

import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.allowance.manager.core.domain.usecase.balance.GetBalanceUseCase
import com.allowance.manager.core.domain.usecase.balance.SetBalanceUseCase
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
        fun getBalanceUseCase(): GetBalanceUseCase
        fun setBalanceUseCase(): SetBalanceUseCase
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java)
    }

    private val getBalanceUseCase get() = entryPoint.getBalanceUseCase()
    private val setBalanceUseCase get() = entryPoint.setBalanceUseCase()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun Bundle.toSimpleString(): String {
        return this.keySet().joinToString(prefix = "{ ", postfix = " }", separator = ", ") { key ->
            "$key=${this.get(key)}"
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification?.extras ?: return
        Timber.e("onNotificationPosted extras : ${extras.toSimpleString()}")

        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text")?.toString()

        val result = NotificationParser.parse(
            packageName = sbn.packageName,
            title = title,
            text = text,
        ) ?: return

        serviceScope.launch {
            deductBalance(result.amount)
        }
    }

    private suspend fun deductBalance(amount: Long) {
        setBalanceUseCase(amount)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
