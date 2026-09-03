package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TxScope
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun record(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): Transaction?

    /** 가장 오래된 내역 시각(epoch ms). 월별 화면의 이전-달 이동 하한. 내역 없으면 null */
    suspend fun getFirstTransactionTime(): Long?

    /** 가장 최근 내역 시각(epoch ms). 가계부 관리 알림의 "안 본 내역" 판정. 내역 없으면 null */
    suspend fun getLastTransactionTime(): Long?

    fun observeAll(): Flow<List<Transaction>>
    fun observeBetween(start: Long, end: Long): Flow<List<Transaction>>

    /** 예산 지출 합계 (scope=BUDGET·EXPENSE, 환불 음수 자동 차감) — 남은 금액·하루지표 */
    fun observeBudgetSpentBetween(start: Long, end: Long): Flow<Long>

    /** 예산 수입 합계 (scope=BUDGET·INCOME) — 남은 금액 가산 */
    fun observeBudgetIncomeBetween(start: Long, end: Long): Flow<Long>

    /** 전체 내역의 시각 목록 (최신순) — 사이클별 거래 유무 판정용 경량 조회 */
    fun observeAllTimes(): Flow<List<Long>>

    /** 가계부 지출 합계 (scope!=EXCLUDED·EXPENSE) — 가계부 위젯 등 집계 */
    fun observeLedgerSpentBetween(start: Long, end: Long): Flow<Long>

    /** 가계부 수입 합계 (scope!=EXCLUDED·INCOME) — 홈 수입 카드·가계부 위젯 */
    fun observeLedgerIncomeBetween(start: Long, end: Long): Flow<Long>



    /** 숨김 토글 (합계 제외/복원, 내역은 보관) — 스와이프 빠른 제외/복원용 */
    suspend fun setHidden(id: Long, hidden: Boolean)

    /** 예산 반영 상태 지정 (BUDGET/LEDGER_ONLY/EXCLUDED) — 상세시트 3분기 컨트롤 */
    suspend fun setScope(id: Long, scope: TxScope)

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
