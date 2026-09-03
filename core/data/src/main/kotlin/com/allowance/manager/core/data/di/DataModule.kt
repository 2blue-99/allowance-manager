package com.allowance.manager.core.data.di

import com.allowance.manager.core.data.repository.AccountRepositoryImpl
import com.allowance.manager.core.data.repository.AppVersionRepositoryImpl
import com.allowance.manager.core.data.repository.BudgetRepositoryImpl
import com.allowance.manager.core.data.repository.PaydayRepositoryImpl
import com.allowance.manager.core.data.repository.DataStoreRepositoryImpl
import com.allowance.manager.core.data.repository.IgnoredAccountRepositoryImpl
import com.allowance.manager.core.data.repository.RemoteConfigRepositoryImpl
import com.allowance.manager.core.data.repository.TransactionRepositoryImpl
import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.domain.repository.AppVersionRepository
import com.allowance.manager.core.domain.repository.BudgetRepository
import com.allowance.manager.core.domain.repository.PaydayRepository
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.IgnoredAccountRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
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
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl,
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl,
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        impl: BudgetRepositoryImpl,
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindIgnoredAccountRepository(
        impl: IgnoredAccountRepositoryImpl,
    ): IgnoredAccountRepository

    @Binds
    @Singleton
    abstract fun bindPaydayRepository(
        impl: PaydayRepositoryImpl,
    ): PaydayRepository
}
