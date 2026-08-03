package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.MonthlyExpense
import com.allowance.manager.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun record(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): Transaction?

    /** 가장 오래된 내역 시각(epoch ms). 월별 화면의 이전-달 이동 하한. 내역 없으면 null */
    suspend fun getFirstTransactionTime(): Long?

    fun observeAll(): Flow<List<Transaction>>
    fun observeBetween(start: Long, end: Long): Flow<List<Transaction>>

    /** 이번달 지출 합계 (메인·숨김아님·EXPENSE, 환불 음수 자동 차감) */
    fun observeSpentBetween(start: Long, end: Long): Flow<Long>

    /** 월별 지출 합계 (통계용) */
    fun observeMonthlyExpenseTotals(): Flow<List<MonthlyExpense>>

    /** 숨김 토글 (합계 제외/복원, 내역은 보관) */
    suspend fun setHidden(id: Long, hidden: Boolean)

    /** 같은 계좌 패턴의 비메인 내역을 메인(accountId)으로 승격 */
    suspend fun promoteToMain(pattern: String, accountId: Long)

    /** 같은 출처(앱)의 계좌번호 없는 비메인 내역을 메인(accountId)으로 승격 */
    suspend fun promoteToMainBySource(packageName: String, accountId: Long)

    /**
     * 무시 계좌 기준 매칭 내역 건수 (다이얼로그 "기존 {n}건" 표시).
     * pattern != null → 계좌번호 자리별 대조 / pattern == null → 계좌번호 없는 같은 출처.
     */
    suspend fun countMatchingForIgnore(pattern: String?, packageName: String): Int

    /** 무시 계좌 등록 시 같은 기준의 기존 내역 전 기간 삭제(소급) */
    suspend fun deleteMatchingForIgnore(pattern: String?, packageName: String)
}
