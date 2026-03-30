package com.allowance.manager.core.domain.usecase.config

import com.allowance.manager.core.domain.repository.AppVersionRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.util.checkForceVersion
import javax.inject.Inject

/**
 * 강제 업데이트 여부 체크해서 Boolean 반환
 */
class CheckForceUpdateUseCase @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val appVersionRepository: AppVersionRepository,
) {
    operator fun invoke(): Boolean {
        val forcedVersion = remoteConfigRepository.getForcedUpdateVersion()
        val currentVersion = appVersionRepository.getVersionName()
        return forcedVersion.checkForceVersion(currentVersion)
    }
}
