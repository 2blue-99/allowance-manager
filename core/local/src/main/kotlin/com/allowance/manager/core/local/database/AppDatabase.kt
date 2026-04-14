package com.allowance.manager.core.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.allowance.manager.core.local.dao.SpendingDao
import com.allowance.manager.core.local.entity.SpendingEntity

@Database(
    entities = [SpendingEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spendingDao(): SpendingDao
}
