package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.IgnoredAccount
import kotlinx.coroutines.flow.Flow

interface IgnoredAccountRepository {
    /** 설정 화면 목록 (최신 등록 순) */
    fun observeAll(): Flow<List<IgnoredAccount>>

    /** 알림 수신 시 매칭 검사용 (전체) */
    suspend fun getAll(): List<IgnoredAccount>

    suspend fun add(account: IgnoredAccount): Long

    suspend fun delete(id: Long)
}
