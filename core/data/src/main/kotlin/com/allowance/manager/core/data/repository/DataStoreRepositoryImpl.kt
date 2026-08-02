package com.allowance.manager.core.data.repository

import com.allowance.manager.core.datastore.PreferencesDataSource
import com.allowance.manager.core.domain.model.HomeFilter
import com.allowance.manager.core.domain.model.UserType
import com.allowance.manager.core.domain.repository.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    override fun getHomeFilter(): Flow<HomeFilter> =
        combine(
            preferencesDataSource.getHomeShowMain(),
            preferencesDataSource.getHomeShowHidden(),
        ) { main, hidden -> HomeFilter(showMain = main, showHidden = hidden) }
    override suspend fun setHomeFilter(filter: HomeFilter) {
        preferencesDataSource.setHomeShowMain(filter.showMain)
        preferencesDataSource.setHomeShowHidden(filter.showHidden)
    }

    override fun getHomeGuideShown(): Flow<Boolean> = preferencesDataSource.getHomeGuideShown()
    override suspend fun setHomeGuideShown(shown: Boolean) = preferencesDataSource.setHomeGuideShown(shown)
}
