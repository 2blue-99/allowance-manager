package com.allowance.manager.core.domain.usecase.config

import com.allowance.manager.core.domain.model.Announcement
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import javax.inject.Inject

/**
 * 지금 홈에서 띄워야 할 공지를 반환한다. 없으면 null.
 *
 * Remote Config 공지가 active이고, 그 id가 "마지막으로 확인한 공지 id"와 다를 때만 반환.
 * (같은 공지를 한 번 확인하면 id가 바뀌기 전까지 다시 안 뜬다.)
 */
class GetPendingAnnouncementUseCase @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(): Announcement? {
        val announcement = remoteConfigRepository.getAnnouncement() ?: return null
        val lastSeenId = dataStoreRepository.getLastSeenAnnouncementId()
        return announcement.takeIf { it.id != lastSeenId }
    }
}
