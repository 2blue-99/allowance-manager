package com.allowance.manager.core.domain.usecase.onboarding

import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetIntroShownUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<Boolean> = dataStoreRepository.getIntroShown()
}

class SetIntroShownUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke() = dataStoreRepository.setIntroShown(true)
}

class GetOnboardingDoneUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    operator fun invoke(): Flow<Boolean> = dataStoreRepository.getOnboardingDone()
}

class SetOnboardingDoneUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke() = dataStoreRepository.setOnboardingDone(true)
}
