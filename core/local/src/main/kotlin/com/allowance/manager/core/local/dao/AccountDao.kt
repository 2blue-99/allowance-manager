package com.allowance.manager.core.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.allowance.manager.core.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(entity: AccountEntity): Long

    @Update
    suspend fun update(entity: AccountEntity)

    @Delete
    suspend fun delete(entity: AccountEntity)

    @Query("SELECT * FROM accounts ORDER BY id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE enabled = 1")
    suspend fun getEnabled(): List<AccountEntity>

    /** 꺼진 계좌 포함 전체 — 승격 시 같은 계좌 재사용(중복 등록 방지)용 */
    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?
}
