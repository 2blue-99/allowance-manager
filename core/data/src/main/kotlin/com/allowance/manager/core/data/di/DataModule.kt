package com.allowance.manager.core.data.di

import com.allowance.manager.core.data.repository.AppVersionRepositoryImpl
import com.allowance.manager.core.data.repository.DataStoreRepositoryImpl
import com.allowance.manager.core.data.repository.RemoteConfigRepositoryImpl
import com.allowance.manager.core.data.repository.SpendingRepositoryImpl
import com.allowance.manager.core.domain.repository.AppVersionRepository
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.repository.SpendingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBalanceRepository(
        impl: DataStoreRepositoryImpl,
    ): DataStoreRepository

    @Binds
    @Singleton
    abstract fun bindRemoteConfigRepository(
        impl: RemoteConfigRepositoryImpl,
    ): RemoteConfigRepository

    @Binds
    @Singleton
    abstract fun bindAppVersionRepository(
        impl: AppVersionRepositoryImpl,
    ): AppVersionRepository

    @Binds
    @Singleton
    abstract fun bindSpendingRepository(
        impl: SpendingRepositoryImpl,
    ): SpendingRepository
}
