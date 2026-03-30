package com.allowance.manager.core.domain.repository

interface RemoteConfigRepository {
    suspend fun fetchAndActivate(): Boolean
    fun getForcedUpdateVersion(): String
    fun getUpdateNote(): String
}
