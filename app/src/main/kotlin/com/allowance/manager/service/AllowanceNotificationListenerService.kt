package com.allowance.manager.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.allowance.manager.core.domain.usecase.balance.GetBalanceUseCase
import com.allowance.manager.core.domain.usecase.balance.SetBalanceUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AllowanceNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var getBalanceUseCase: GetBalanceUseCase
    @Inject
    lateinit var setBalanceUseCase: SetBalanceUseCase

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
            deductBalance(result.amount)
        }
    }

    private suspend fun deductBalance(amount: Long) {
        val current = getBalanceUseCase().first()
        setBalanceUseCase(current - amount)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
