package com.allowance.manager.core.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spending")
data class SpendingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val amount: Long,
    @ColumnInfo(name = "total_amount")
    val totalAmount: Long,
    @ColumnInfo(name = "bank_name")
    val bankName: String = "",
    val timestamp: Long,
    val memo: String = "",
)
