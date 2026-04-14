package com.allowance.manager.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.allowance.manager.core.local.entity.SpendingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpendingDao {
    @Insert
    suspend fun insert(entity: SpendingEntity)

    @Query("SELECT * FROM spending ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SpendingEntity>>
}
