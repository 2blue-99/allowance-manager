package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.PaydayRule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 월급일 이력. 규칙일과 "그 규칙이 시작된 날"을 함께 보관해, 규칙을 바꿔도 과거 사이클이 흔들리지 않게 한다.
 *
 * 기존 `DataStoreRepository.getPayday()`(값 하나)를 대체한다.
 */
interface PaydayRepository {
    /**
     * [date] 시점에 유효한 규칙.
     *
     * 이력이 아직 없으면(온보딩 이전, 또는 DB만 초기화된 개발 기기) DataStore의 규칙일로 대신한다.
     * 그래서 호출부는 "이력 없음" 분기를 두지 않아도 된다.
     */
    suspend fun ruleAt(date: LocalDate): PaydayRule

    /** [date] 시점 규칙 Flow — 이력 변경 시 재방출. 대체 규칙은 [ruleAt]과 같다. */
    fun observeRuleAt(date: LocalDate): Flow<PaydayRule>

    /** 전체 이력(오래된 → 최신). 사이클 목록·피커 계산에 사용. */
    fun observeHistory(): Flow<List<PaydayRule>>

    /** 전체 이력 1회 조회. */
    suspend fun history(): List<PaydayRule>

    /**
     * 월급일 변경 저장. [effectiveDate]부터 새 사이클이 시작하고, 이후 경계는 [payday]로 계산한다.
     * 같은 날짜에 다시 저장하면 덮어쓴다. 보존 한도를 넘으면 오래된 것부터 버린다.
     */
    suspend fun setRule(effectiveDate: LocalDate, payday: Int)

    /** 그 날짜의 이력 삭제(경계 지정 취소). */
    suspend fun removeRule(effectiveDate: LocalDate)
}
