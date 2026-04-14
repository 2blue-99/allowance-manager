package com.allowance.manager.service

import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.allowance.manager.core.domain.model.Spending
import com.allowance.manager.core.domain.usecase.spending.InsertSpendingUseCase
import com.allowance.manager.core.domain.usecase.store.GetMonthAllowanceUseCase
import com.allowance.manager.core.domain.usecase.store.SetMonthAllowanceUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class AllowanceNotificationListenerService : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceEntryPoint {
        fun setBalanceUseCase(): SetMonthAllowanceUseCase
        fun getBalanceUseCase(): GetMonthAllowanceUseCase
        fun insertSpendingUseCase(): InsertSpendingUseCase
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java)
    }

    private val setBalanceUseCase get() = entryPoint.setBalanceUseCase()
    private val getBalanceUseCase get() = entryPoint.getBalanceUseCase()
    private val insertSpendingUseCase get() = entryPoint.insertSpendingUseCase()

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
            val newBalance = result.amount
            val previousBalance = getBalanceUseCase().first()
            val spendingAmount = previousBalance - newBalance

            Timber.e("newBalance : $newBalance, previousBalance : $previousBalance, spendingAmount : $spendingAmount")

            insertSpendingUseCase(
                Spending(
                    amount = spendingAmount,
                    remainingBalance = newBalance,
                    timestamp = System.currentTimeMillis(),
                )
            )

            setBalanceUseCase(newBalance)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
