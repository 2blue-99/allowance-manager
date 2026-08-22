package com.allowance.manager.core.domain.usecase.alert

import com.allowance.manager.core.domain.model.BudgetAlertSetting
import com.allowance.manager.core.domain.model.DailyReminderSetting
import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBudgetAlertSettingUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<BudgetAlertSetting> = dataStoreRepository.getBudgetAlertSetting()
}

class SetBudgetAlertSettingUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(setting: BudgetAlertSetting) =
        dataStoreRepository.setBudgetAlertSetting(setting)
}

class GetDailyReminderSettingUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<DailyReminderSetting> = dataStoreRepository.getDailyReminderSetting()
}

class SetDailyReminderSettingUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(setting: DailyReminderSetting) =
        dataStoreRepository.setDailyReminderSetting(setting)
}

/** 앱을 조회한 시각을 기록 — 가계부 관리 알림의 "안 본 내역" 판정 기준(lastSeen) 갱신. */
class MarkAppSeenUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke() =
        dataStoreRepository.setReminderLastSeen(System.currentTimeMillis())
}
