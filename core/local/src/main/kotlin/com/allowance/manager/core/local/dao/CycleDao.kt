package com.allowance.manager.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.allowance.manager.core.local.entity.CycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {

    /** 행 추가/교체 (같은 start면 덮어씀) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CycleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CycleEntity>)

    /** 전체 사이클 (오래된 → 최신). 관찰 — 행 변경 시 재방출 */
    @Query("SELECT * FROM cycles ORDER BY start ASC")
    fun observeAll(): Flow<List<CycleEntity>>

    /** 전체 사이클 1회 조회 (오래된 → 최신) */
    @Query("SELECT * FROM cycles ORDER BY start ASC")
    suspend fun getAll(): List<CycleEntity>

    /** [start] 이후에 시작하는 행 삭제 — 월급일 변경 시 대체되는 사이클을 걷어낸다 */
    @Query("DELETE FROM cycles WHERE start >= :start")
    suspend fun deleteStartingFrom(start: String)

    /** 경계 갱신 — 마지막 행 끝(예정) 재계산, 또는 변경 시 이전 행 끝을 새 경계로 */
    @Query("UPDATE cycles SET end_exclusive = :end, updated_at = :updatedAt WHERE start = :start")
    suspend fun updateEnd(start: String, end: String, updatedAt: Long)

    /** 그 사이클 예산 변경 */
    @Query("UPDATE cycles SET budget = :budget, updated_at = :updatedAt WHERE start = :start")
    suspend fun updateBudget(start: String, budget: Long, updatedAt: Long)
}
