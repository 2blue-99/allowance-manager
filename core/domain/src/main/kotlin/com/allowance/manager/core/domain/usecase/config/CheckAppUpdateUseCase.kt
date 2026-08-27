package com.allowance.manager.core.domain.usecase.config

import com.allowance.manager.core.domain.model.UpdateType
import com.allowance.manager.core.domain.repository.AppVersionRepository
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.util.checkForceVersion
import java.time.LocalDate
import javax.inject.Inject

/**
 * 지금 띄워야 할 업데이트 팝업 종류를 반환한다. 없으면 null.
 *
 * - 앱 버전 < `update_forced_version` → [UpdateType.FORCED] (우선)
 * - 앱 버전 < `update_recommend_version` → [UpdateType.RECOMMEND] (단, 오늘 이미 봤으면 제외)
 *
 * "하루 1회"는 마지막으로 추천 팝업을 띄운 날짜와 오늘을 비교해 판정한다.
 */
class CheckAppUpdateUseCase @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val appVersionRepository: AppVersionRepository,
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): UpdateType? {
        val current = appVersionRepository.getVersionName()

        if (remoteConfigRepository.getForcedUpdateVersion().checkForceVersion(current)) {
            return UpdateType.FORCED
        }

        if (remoteConfigRepository.getRecommendUpdateVersion().checkForceVersion(current)) {
            val shownToday = dataStoreRepository.getRecommendUpdateLastShown() == today.toString()
            if (!shownToday) return UpdateType.RECOMMEND
        }

        return null
    }
}
