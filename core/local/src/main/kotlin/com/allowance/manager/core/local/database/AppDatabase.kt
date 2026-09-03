package com.allowance.manager.core.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.allowance.manager.core.local.dao.AccountDao
import com.allowance.manager.core.local.dao.CycleDao
import com.allowance.manager.core.local.dao.IgnoredAccountDao
import com.allowance.manager.core.local.dao.TransactionDao
import com.allowance.manager.core.local.entity.AccountEntity
import com.allowance.manager.core.local.entity.CycleEntity
import com.allowance.manager.core.local.entity.IgnoredAccountEntity
import com.allowance.manager.core.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CycleEntity::class,
        IgnoredAccountEntity::class,
    ],
    // v13: 사이클 실체화 — payday_history·budget_history → cycles 테이블 통합,
    //      transactions.cycle_start(도장) 제거(소속은 createdAt 범위로 조회)
    version = 13,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun cycleDao(): CycleDao
    abstract fun ignoredAccountDao(): IgnoredAccountDao
}
