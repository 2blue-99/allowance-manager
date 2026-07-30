package com.allowance.manager.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.allowance.manager.core.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    /** 용돈 변경 upsert (같은 effectiveMonth면 교체) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BudgetEntity)

    /** 그 달(month="yyyy-MM") 이하 최신 용돈. 없으면 null (미설정) */
    @Query("SELECT amount FROM budget_history WHERE effective_month <= :month ORDER BY effective_month DESC LIMIT 1")
    suspend fun getBudgetForMonth(month: String): Long?

    /** 위와 동일하되 이력 변경 시 재방출 (홈 등 실시간 반영) */
    @Query("SELECT amount FROM budget_history WHERE effective_month <= :month ORDER BY effective_month DESC LIMIT 1")
    fun observeBudgetForMonth(month: String): Flow<Long?>

    /** 전체 이력 (오래된 → 최신). 통계 6개월 창에 용돈 매핑 */
    @Query("SELECT * FROM budget_history ORDER BY effective_month ASC")
    fun observeAll(): Flow<List<BudgetEntity>>
}
