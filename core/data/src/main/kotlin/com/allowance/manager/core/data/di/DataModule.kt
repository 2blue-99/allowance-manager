package com.allowance.manager.core.data.di

import com.allowance.manager.core.data.repository.AppVersionRepositoryImpl
import com.allowance.manager.core.data.repository.BalanceRepositoryImpl
import com.allowance.manager.core.data.repository.RemoteConfigRepositoryImpl
import com.allowance.manager.core.domain.repository.AppVersionRepository
import com.allowance.manager.core.domain.repository.BalanceRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
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
        impl: BalanceRepositoryImpl,
    ): BalanceRepository

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
}
