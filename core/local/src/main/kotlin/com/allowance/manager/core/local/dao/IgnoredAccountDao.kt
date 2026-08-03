package com.allowance.manager.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.allowance.manager.core.local.entity.IgnoredAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredAccountDao {
    @Insert
    suspend fun insert(entity: IgnoredAccountEntity): Long

    /** 설정 화면 목록 (최신 등록 순) */
    @Query("SELECT * FROM ignored_accounts ORDER BY created_at DESC")
    fun observeAll(): Flow<List<IgnoredAccountEntity>>

    /** 알림 수신 시 매칭 검사용 (전체 로드) */
    @Query("SELECT * FROM ignored_accounts")
    suspend fun getAll(): List<IgnoredAccountEntity>

    @Query("DELETE FROM ignored_accounts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
