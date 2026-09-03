package com.allowance.manager.core.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 수급일 사이클 한 개 — **받은 날(포함) ~ 다음 받는 날(미포함)** + 그 사이클의 예산.
 *
 * 지나간 사이클은 "일어난 사실"이라 행으로 얼려두고, 규칙은 다음 행을 만들 때만 쓴다.
 * 새 행은 관문(CycleRepository.ensure)이 사이클을 읽는 길목에서 이어서 생성한다.
 *
 * - start: "yyyy-MM-dd" 받은 날. 문자열 정렬 = 시간 정렬. 사이클끼리 겹치지 않아 자연키.
 * - endExclusive: 다음 받는 날. **마지막 행의 끝만 '예정'** — 규칙·공휴일이 바뀌면 재계산된다.
 *   인접성(끝 = 다음 행의 시작)은 관문·변경 로직이 유지한다.
 * - budget: 이 사이클 예산. 새 행 생성 시 직전 행 값을 복사(이월).
 * - payday: 규칙일(1~31, 0=말일). **최신 행의 값이 곧 현재 규칙**(단일 소스).
 *
 * 거래는 이 테이블을 참조하지 않는다 — 소속은 createdAt 날짜 범위로 조회한다.
 */
@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey
    @ColumnInfo(name = "start")
    val start: String,
    @ColumnInfo(name = "end_exclusive")
    val endExclusive: String,
    val budget: Long,
    val payday: Int,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
