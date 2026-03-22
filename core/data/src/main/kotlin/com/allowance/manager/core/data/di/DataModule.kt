package com.allowance.manager.core.data.di

import com.allowance.manager.core.data.repository.BalanceRepositoryImpl
import com.allowance.manager.core.domain.repository.BalanceRepository
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
}
