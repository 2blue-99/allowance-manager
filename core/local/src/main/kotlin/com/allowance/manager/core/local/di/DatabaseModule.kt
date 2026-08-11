package com.allowance.manager.core.local.di

import android.content.Context
import androidx.room.Room
import com.allowance.manager.core.local.dao.AccountDao
import com.allowance.manager.core.local.dao.BudgetDao
import com.allowance.manager.core.local.dao.IgnoredAccountDao
import com.allowance.manager.core.local.dao.TransactionDao
import com.allowance.manager.core.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "allowance_manager.db",
    )
        // v1.0.0 릴리즈: 파괴적 재생성 제거 → 이후 스키마 변경은 반드시 실제 Migration 추가.
        .build()

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao =
        database.accountDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao =
        database.budgetDao()

    @Provides
    fun provideIgnoredAccountDao(database: AppDatabase): IgnoredAccountDao =
        database.ignoredAccountDao()
}
