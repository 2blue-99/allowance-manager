package com.allowance.manager.core.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.allowance.manager.core.local.dao.AccountDao
import com.allowance.manager.core.local.dao.TransactionDao
import com.allowance.manager.core.local.entity.AccountEntity
import com.allowance.manager.core.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
}
