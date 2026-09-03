package com.allowance.manager.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.allowance.manager.core.local.entity.PaydayHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaydayHistoryDao {

    /** 월급일 변경 upsert (같은 effectiveDate면 교체) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PaydayHistoryEntity)

    /** 그 날짜(date="yyyy-MM-dd") 이하 최신 규칙. 없으면 null (미설정) */
    @Query("SELECT * FROM payday_history WHERE effective_date <= :date ORDER BY effective_date DESC LIMIT 1")
    suspend fun ruleAt(date: String): PaydayHistoryEntity?

    /** 위와 동일하되 이력 변경 시 재방출 (홈 등 실시간 반영) */
    @Query("SELECT * FROM payday_history WHERE effective_date <= :date ORDER BY effective_date DESC LIMIT 1")
    fun observeRuleAt(date: String): Flow<PaydayHistoryEntity?>

    /** 전체 이력 (오래된 → 최신). 사이클 목록 계산·피커에 사용 */
    @Query("SELECT * FROM payday_history ORDER BY effective_date ASC")
    fun observeAll(): Flow<List<PaydayHistoryEntity>>

    /** 전체 이력 1회 조회 */
    @Query("SELECT * FROM payday_history ORDER BY effective_date ASC")
    suspend fun getAll(): List<PaydayHistoryEntity>

    /** [date]보다 뒤(아직 오지 않은) 이력 삭제 — 규칙을 다시 바꿀 때 예정분을 걷어낸다 */
    @Query("DELETE FROM payday_history WHERE effective_date > :date")
    suspend fun deleteAfter(date: String)

    /** 그 날짜의 지정 해제 */
    @Query("DELETE FROM payday_history WHERE effective_date = :date")
    suspend fun delete(date: String)

    /** 이력 보존 한도를 넘은 오래된 행 삭제 (최근 [keep]개만 남긴다) */
    @Query(
        """
        DELETE FROM payday_history WHERE effective_date NOT IN (
            SELECT effective_date FROM payday_history ORDER BY effective_date DESC LIMIT :keep
        )
        """
    )
    suspend fun trimTo(keep: Int)

    @Query("SELECT COUNT(*) FROM payday_history")
    suspend fun count(): Int
}
