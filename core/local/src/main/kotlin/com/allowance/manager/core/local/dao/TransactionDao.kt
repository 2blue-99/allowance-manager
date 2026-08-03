package com.allowance.manager.core.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.allowance.manager.core.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Delete
    suspend fun delete(entity: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    /** 가장 오래된 내역 시각 (월별 화면의 이전-달 이동 하한). 내역이 없으면 null */
    @Query("SELECT MIN(created_at) FROM transactions")
    suspend fun getFirstTransactionTime(): Long?

    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE created_at BETWEEN :start AND :end ORDER BY created_at DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    /** 이번달(사이클) 지출 합계 = 메인 · 숨김 아님 · EXPENSE (환불 음수 자동 차감) */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'EXPENSE'
          AND (account_id IS NOT NULL OR is_manual = 1)
          AND is_hidden = 0
          AND created_at BETWEEN :start AND :end
        """
    )
    fun observeSpentBetween(start: Long, end: Long): Flow<Long>

    /**
     * 아직 메인 계좌에 매칭되지 않은 내역 (승격 소급 적용 후보).
     * 마스킹 계좌 대조는 SQL로 표현할 수 없어 도메인(MaskedAccount)에서 판정한다.
     */
    @Query("SELECT * FROM transactions WHERE account_id IS NULL")
    suspend fun getUnmatched(): List<TransactionEntity>

    /** 비메인 → 메인 승격: 지정한 내역들에 accountId 소급 적용 */
    @Query("UPDATE transactions SET account_id = :accountId WHERE id IN (:ids)")
    suspend fun promoteByIds(ids: List<Long>, accountId: Long)

    // ── 무시 계좌 소급 삭제·건수 ──
    /** 계좌번호 있는 내역 (번호 기준 무시 매칭 후보; 마스킹 대조는 도메인에서 판정) */
    @Query("SELECT * FROM transactions WHERE extracted_account IS NOT NULL AND extracted_account != ''")
    suspend fun getWithExtractedAccount(): List<TransactionEntity>

    /** 지정 내역 일괄 삭제 (번호 기준 무시 소급) */
    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** 출처(앱) 기준 무시: 같은 packageName의 계좌번호 없는 내역 건수 */
    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE package_name = :packageName
          AND (extracted_account IS NULL OR extracted_account = '')
        """
    )
    suspend fun countBySource(packageName: String): Int

    /** 출처(앱) 기준 무시: 같은 packageName의 계좌번호 없는 내역 삭제 */
    @Query(
        """
        DELETE FROM transactions
        WHERE package_name = :packageName
          AND (extracted_account IS NULL OR extracted_account = '')
        """
    )
    suspend fun deleteBySource(packageName: String)

    /**
     * 출처(앱) 기준 승격: 같은 packageName의 **계좌번호 없는** 미매칭 내역에 accountId 소급 적용.
     * (계좌번호 있는 내역은 번호 매칭 대상이므로 제외)
     */
    @Query(
        """
        UPDATE transactions SET account_id = :accountId
        WHERE account_id IS NULL
          AND package_name = :packageName
          AND (extracted_account IS NULL OR extracted_account = '')
        """
    )
    suspend fun promoteBySource(packageName: String, accountId: Long)

    /** 월별 지출 합계 (메인·숨김아님·EXPENSE). ym = "yyyy-MM" (기기 로컬 타임존 기준) */
    @Query(
        """
        SELECT strftime('%Y-%m', created_at / 1000, 'unixepoch', 'localtime') AS ym,
               COALESCE(SUM(amount), 0) AS total
        FROM transactions
        WHERE type = 'EXPENSE'
          AND (account_id IS NOT NULL OR is_manual = 1)
          AND is_hidden = 0
        GROUP BY ym
        """
    )
    fun observeMonthlyExpenseTotals(): Flow<List<MonthlyTotalRow>>
}
