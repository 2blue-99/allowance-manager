package com.allowance.manager.core.data.repository

import com.allowance.manager.core.config.RemoteConfigDataSource
import com.allowance.manager.core.config.RemoteConfigKeys
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigRepositoryImpl @Inject constructor(
    private val remoteConfigDataSource: RemoteConfigDataSource,
) : RemoteConfigRepository {

    override suspend fun fetchAndActivate(): Boolean =
        remoteConfigDataSource.fetchAndActivate()

    override fun getForcedUpdateVersion(): String =
        remoteConfigDataSource.getString(RemoteConfigKeys.FORCED_UPDATE_VERSION)

    override fun getUpdateNote(): String =
        remoteConfigDataSource.getString(RemoteConfigKeys.UPDATE_NOTE)
}
