package com.allowance.manager.core.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 무시한 출처/계좌. 매칭되는 알림은 파싱 후 저장하지 않고 드롭한다.
 * - account_pattern 있으면: 마스킹 계좌번호 자리별 대조 (은행, 그 계좌만)
 * - account_pattern 비면: 출처(package_name)로 대조 (카드·메신저 등, 그 앱 전체)
 */
@Entity(tableName = "ignored_accounts")
data class IgnoredAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "source_name")
    val sourceName: String,
    @ColumnInfo(name = "account_pattern")
    val accountPattern: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
