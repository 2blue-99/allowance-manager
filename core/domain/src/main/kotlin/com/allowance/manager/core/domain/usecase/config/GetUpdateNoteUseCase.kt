package com.allowance.manager.core.domain.usecase.config

import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import javax.inject.Inject

class GetUpdateNoteUseCase @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    operator fun invoke(): String = remoteConfigRepository.getUpdateNote()
}
