package com.allowance.manager.core.domain.usecase.setting

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

class GetShowMainOnlyUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<Boolean> = dataStoreRepository.getShowMainOnly()
}

class SetShowMainOnlyUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(mainOnly: Boolean) = dataStoreRepository.setShowMainOnly(mainOnly)
}
