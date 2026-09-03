package com.allowance.manager.core.local.di

import android.content.Context
import androidx.room.Room
import com.allowance.manager.core.local.dao.AccountDao
import com.allowance.manager.core.local.dao.BudgetDao
import com.allowance.manager.core.local.dao.IgnoredAccountDao
import com.allowance.manager.core.local.dao.PaydayHistoryDao
import com.allowance.manager.core.local.dao.TransactionDao
import com.allowance.manager.core.local.database.AppDatabase
import com.allowance.manager.core.local.database.MIGRATION_9_10
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
        .addMigrations(MIGRATION_9_10)
        // 마이그레이션 경로가 없을 때만 파괴적 재생성(개발용). 릴리즈 전 제거 예정.
        .fallbackToDestructiveMigration()
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

    @Provides
    fun providePaydayHistoryDao(database: AppDatabase): PaydayHistoryDao =
        database.paydayHistoryDao()
}
