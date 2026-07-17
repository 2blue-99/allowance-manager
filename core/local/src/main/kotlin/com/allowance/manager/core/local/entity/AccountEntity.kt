package com.allowance.manager.core.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자가 등록한 메인 계좌.
 * 알림 텍스트에 accountPattern(마스킹 계좌번호)이 포함되면 해당 계좌 거래로 인식.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "bank_name")
    val bankName: String,
    @ColumnInfo(name = "account_pattern")
    val accountPattern: String,
    val enabled: Boolean = true,
)
