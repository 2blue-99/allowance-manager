package com.allowance.manager.core.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.allowance.manager.core.local.dao.AccountDao
import com.allowance.manager.core.local.dao.BudgetDao
import com.allowance.manager.core.local.dao.IgnoredAccountDao
import com.allowance.manager.core.local.dao.PaydayHistoryDao
import com.allowance.manager.core.local.dao.TransactionDao
import com.allowance.manager.core.local.entity.AccountEntity
import com.allowance.manager.core.local.entity.BudgetEntity
import com.allowance.manager.core.local.entity.IgnoredAccountEntity
import com.allowance.manager.core.local.entity.PaydayHistoryEntity
import com.allowance.manager.core.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        IgnoredAccountEntity::class,
        PaydayHistoryEntity::class,
    ],
    version = 11,  // v11: payday_history(월급일 이력) 테이블 추가 — 규칙 변경 시 과거 사이클 보존
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun ignoredAccountDao(): IgnoredAccountDao
    abstract fun paydayHistoryDao(): PaydayHistoryDao
}
