package com.allowance.manager.core.domain.usecase.config

import com.allowance.manager.core.domain.repository.DataStoreRepository
import javax.inject.Inject

/** 공지를 확인(닫음)했다고 기록 — 그 id를 저장해 같은 공지가 다시 안 뜨게 한다. */
class MarkAnnouncementSeenUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(id: String) {
        dataStoreRepository.setLastSeenAnnouncementId(id)
    }
}
