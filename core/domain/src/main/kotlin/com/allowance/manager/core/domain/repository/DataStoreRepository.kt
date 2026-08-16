package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.LedgerFilter
import com.allowance.manager.core.domain.model.UserType
import kotlinx.coroutines.flow.Flow

interface DataStoreRepository {
    // 용돈(월 예산)은 budget_history 테이블(월별 이력)로 이관 — BudgetRepository 참조

    fun getPayday(): Flow<Int>          // 1~31, 0 = 말일
    suspend fun setPayday(day: Int)

    /** 사용자 유형(student/youth/common) — 온보딩·설정에서 선택, 개인화에 사용 */
    fun getUserType(): Flow<UserType>
    suspend fun setUserType(type: UserType)

    fun getIntroShown(): Flow<Boolean>
    suspend fun setIntroShown(shown: Boolean)

    fun getOnboardingDone(): Flow<Boolean>
    suspend fun setOnboardingDone(done: Boolean)

    fun getStatusBarEnabled(): Flow<Boolean>
    suspend fun setStatusBarEnabled(enabled: Boolean)

    /** 홈 내역 필터 (메인/숨김/전체) */
    fun getHomeFilter(): Flow<LedgerFilter>
    suspend fun setHomeFilter(filter: LedgerFilter)

    /** 월별 내역 필터 (홈과 분리 저장) */
    fun getCalendarFilter(): Flow<LedgerFilter>
    suspend fun setCalendarFilter(filter: LedgerFilter)

    /** 홈 최초 진입 가이드(스포트라이트) 노출 완료 여부 */
    fun getHomeGuideShown(): Flow<Boolean>
    suspend fun setHomeGuideShown(shown: Boolean)

    /** 새(미등록) 계좌 감지 배지 — 전체 칩 빨간 점. 전체를 보면 해제 */
    fun getHomeNewAccountBadge(): Flow<Boolean>
    suspend fun setHomeNewAccountBadge(show: Boolean)
}
