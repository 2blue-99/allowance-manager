package com.allowance.manager.core.domain.repository

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

    fun getShowMainOnly(): Flow<Boolean>
    suspend fun setShowMainOnly(mainOnly: Boolean)
}
