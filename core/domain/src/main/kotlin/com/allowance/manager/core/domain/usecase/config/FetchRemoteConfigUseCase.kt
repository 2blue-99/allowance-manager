package com.allowance.manager.core.domain.usecase.config

import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import javax.inject.Inject

class FetchRemoteConfigUseCase @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    suspend operator fun invoke(): Boolean = remoteConfigRepository.fetchAndActivate()
}
