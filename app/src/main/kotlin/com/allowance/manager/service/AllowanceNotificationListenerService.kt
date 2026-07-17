package com.allowance.manager.service

import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.allowance.manager.core.domain.model.Spending
import com.allowance.manager.core.domain.usecase.spending.InsertSpendingUseCase
import com.allowance.manager.core.domain.usecase.store.ChangeDailyAllowanceUseCase
import com.allowance.manager.core.domain.usecase.store.GetDailyAllowanceUseCase
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
        fun changeDailyAllowance(): ChangeDailyAllowanceUseCase

        fun getDailyAllowanceUseCase(): GetDailyAllowanceUseCase

        fun setBalanceUseCase(): SetMonthAllowanceUseCase
        fun getBalanceUseCase(): GetMonthAllowanceUseCase
        fun insertSpendingUseCase(): InsertSpendingUseCase
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java)
    }

    private val changeDailyAllowanceUseCase get() = entryPoint.changeDailyAllowance()

    private val getDailyAllowanceUseCase get() = entryPoint.getDailyAllowanceUseCase()

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
            val isInitDailyAllowance = getDailyAllowanceUseCase().first() != 0L
            // 일일 사용량이 초기화 됐을때만 변경사항 반영
            if(isInitDailyAllowance){
                changeDailyAllowanceUseCase(result.amount)
            }

            // Room db에 저장
            insertSpendingUseCase(
                Spending(
                    type = result.type,
                    amount = result.amount,
                    totalAmount = result.totalAmount,
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
