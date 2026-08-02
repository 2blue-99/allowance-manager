package com.allowance.manager.core.domain.usecase.setting

import com.allowance.manager.core.domain.model.LedgerFilter
import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStatusBarEnabledUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<Boolean> = dataStoreRepository.getStatusBarEnabled()
}

class SetStatusBarEnabledUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = dataStoreRepository.setStatusBarEnabled(enabled)
}

class GetHomeFilterUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<LedgerFilter> = dataStoreRepository.getHomeFilter()
}

class SetHomeFilterUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(filter: LedgerFilter) = dataStoreRepository.setHomeFilter(filter)
}

class GetCalendarFilterUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<LedgerFilter> = dataStoreRepository.getCalendarFilter()
}

class SetCalendarFilterUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(filter: LedgerFilter) = dataStoreRepository.setCalendarFilter(filter)
}

/** 홈 최초 진입 가이드 노출 여부 (false면 아직 안 봄 → 가이드 표시) */
class GetHomeGuideShownUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<Boolean> = dataStoreRepository.getHomeGuideShown()
}

class SetHomeGuideShownUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(shown: Boolean) = dataStoreRepository.setHomeGuideShown(shown)
}
