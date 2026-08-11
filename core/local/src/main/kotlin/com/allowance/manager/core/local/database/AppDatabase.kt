package com.allowance.manager.core.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.allowance.manager.core.local.dao.AccountDao
import com.allowance.manager.core.local.dao.BudgetDao
import com.allowance.manager.core.local.dao.IgnoredAccountDao
import com.allowance.manager.core.local.dao.TransactionDao
import com.allowance.manager.core.local.entity.AccountEntity
import com.allowance.manager.core.local.entity.BudgetEntity
import com.allowance.manager.core.local.entity.IgnoredAccountEntity
import com.allowance.manager.core.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        IgnoredAccountEntity::class,
    ],
    version = 1,    // v1.0.0 릴리즈 baseline (개발 중 누적 마이그레이션 폐기, 최종 스키마로 시작)
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun ignoredAccountDao(): IgnoredAccountDao
}
