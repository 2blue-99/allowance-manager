package com.allowance.manager.core.domain.usecase.config

import com.allowance.manager.core.domain.repository.DataStoreRepository
import java.time.LocalDate
import javax.inject.Inject

/** 추천 업데이트 팝업을 오늘 띄웠다고 기록 — 같은 날 다시 안 뜨게 한다(하루 1회). */
class MarkRecommendUpdateShownUseCase @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()) {
        dataStoreRepository.setRecommendUpdateLastShown(today.toString())
    }
}
