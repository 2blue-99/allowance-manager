package com.allowance.manager.core.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * DB 마이그레이션 모음.
 *
 * ⚠️ 개발용 — 릴리즈(v1.0.0) 때는 version=1로 리셋하고 이 파일과 fallbackToDestructiveMigration을 제거한다.
 * 그때 merchant 등은 v1 스키마에 처음부터 포함된다.
 */

/** v9 → v10: transactions.merchant(사용처) 컬럼 추가. 기존 행은 NULL → 리스트에서 sourceName(출처)로 대체 노출. */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN merchant TEXT")
    }
}
