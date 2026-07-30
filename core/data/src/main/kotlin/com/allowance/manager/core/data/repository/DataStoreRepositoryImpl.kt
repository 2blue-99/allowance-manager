package com.allowance.manager.core.data.repository

import com.allowance.manager.core.datastore.PreferencesDataSource
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
) : DataStoreRepository {

    override fun getPayday(): Flow<Int> = preferencesDataSource.getPayday()
    override suspend fun setPayday(day: Int) = preferencesDataSource.setPayday(day)

    override fun getUserType(): Flow<UserType> =
        preferencesDataSource.getUserType().map { UserType.fromKey(it) }
    override suspend fun setUserType(type: UserType) =
        preferencesDataSource.setUserType(type.key)

    override fun getIntroShown(): Flow<Boolean> = preferencesDataSource.getIntroShown()
    override suspend fun setIntroShown(shown: Boolean) = preferencesDataSource.setIntroShown(shown)

    override fun getOnboardingDone(): Flow<Boolean> = preferencesDataSource.getOnboardingDone()
    override suspend fun setOnboardingDone(done: Boolean) = preferencesDataSource.setOnboardingDone(done)

    override fun getStatusBarEnabled(): Flow<Boolean> = preferencesDataSource.getStatusBarEnabled()
    override suspend fun setStatusBarEnabled(enabled: Boolean) = preferencesDataSource.setStatusBarEnabled(enabled)

    override fun getShowMainOnly(): Flow<Boolean> = preferencesDataSource.getShowMainOnly()
    override suspend fun setShowMainOnly(mainOnly: Boolean) = preferencesDataSource.setShowMainOnly(mainOnly)
}
