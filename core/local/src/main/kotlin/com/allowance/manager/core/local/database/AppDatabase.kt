package com.allowance.manager.core.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.allowance.manager.core.local.dao.TestDao
import com.allowance.manager.core.local.entity.TestEntity

@Database(
    entities = [TestEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun testDao(): TestDao
}
