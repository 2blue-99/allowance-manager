package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.PaydayRule
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.PaydayRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/** [date] 시점에 유효한 월급일 규칙 관찰. */
class ObservePaydayRuleUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
) {
    operator fun invoke(date: LocalDate = LocalDate.now()): Flow<PaydayRule> =
        paydayRepository.observeRuleAt(date)
}

/** 전체 월급일 이력(오래된 → 최신). 사이클 목록·피커 계산용. */
class ObservePaydayHistoryUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
) {
    operator fun invoke(): Flow<List<PaydayRule>> = paydayRepository.observeHistory()
}

/**
 * 월급일 변경 저장 — [effectiveDate]부터 새 사이클이 시작하고, 이후 경계는 [payday]로 계산한다.
 *
 * 규칙일만 바꾸든(칩), 받은 날만 옮기든(캘린더), 둘 다 하든 이 한 곳으로 들어온다.
 * 규칙일은 DataStore에도 함께 반영해 온보딩·설정 표시와 어긋나지 않게 한다.
 */
class SetPaydayRuleUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
    private val dataStoreRepository: DataStoreRepository,
) {
    suspend operator fun invoke(effectiveDate: LocalDate, payday: Int) {
        paydayRepository.setRule(effectiveDate, payday)
        dataStoreRepository.setPayday(payday)
    }
}
