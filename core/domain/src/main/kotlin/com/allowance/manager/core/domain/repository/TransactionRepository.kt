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

    /** 이번달 지출 합계 (메인·무시아님·EXPENSE, 환불 음수 자동 차감) */
    fun observeSpentBetween(start: Long, end: Long): Flow<Long>

    /** 월별 지출 합계 (통계용) */
    fun observeMonthlyExpenseTotals(): Flow<List<MonthlyExpense>>

    suspend fun setIgnored(id: Long, ignored: Boolean)

    /** 같은 계좌 패턴의 비메인 내역을 메인(accountId)으로 승격 */
    suspend fun promoteToMain(pattern: String, accountId: Long)
}
