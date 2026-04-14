package com.allowance.manager.core.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spending")
data class SpendingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Long,
    @ColumnInfo(name = "remaining_balance")
    val remainingBalance: Long,
    val timestamp: Long,
    val category: String = "",
    val memo: String = "",
)
